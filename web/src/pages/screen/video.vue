<script setup lang="ts">
import { castingService } from "@/api/castingService";
import {
  computed,
  nextTick,
  onMounted,
  onUnmounted,
  ref,
  shallowRef,
} from "vue";
import { fetchConfig } from "@/api/admin";
import LogoutButton from "@/components/LogoutButton.vue";

// 视频缓存项接口
interface VideoCacheItem {
  id: string;
  blobUrl: string;
  poster: string;
  lastUsed: number; // 最后使用时间戳
  size: number; // 视频大小（字节）
  url?: string; // 原始URL，用于blob URL失效时的回退
}

// 视频项接口
interface VideoItem {
  id: string;
  url: string;
  poster: string;
  ready: boolean;
  blobUrl?: string; // 当前使用的Blob URL
  startTime?: number; // 视频开始播放的时间戳
  hasPlayed?: boolean; // 是否已经正常播放过
  lastRecoveryAttempt?: number; // 最后恢复尝试时间
  lastErrorTime?: number; // 最后错误时间
}

// 视频格子接口
interface VideoCell {
  current: VideoItem | null;
  next: VideoItem | null;
  transitioning: boolean;
  lastReloadTime?: number; // 最后重新加载时间
}

// 配置参数
const gridSize = ref(3); // 默认3x3网格
const containerAspectRatio = ref<[number, number]>([9, 16]); // 容器宽高比，默认9:16

// 获取容器宽高比配置
async function fetchContainerAspectRatio(): Promise<[number, number]> {
  try {
    const config = await fetchConfig();
    return [config.screenVideoRatios.first, config.screenVideoRatios.second];
  } catch (error) {
    console.error("获取视频屏幕配置失败，使用默认宽高比:", error);
    return [9, 16]; // 默认宽高比
  }
}

// 计算容器宽度的computed属性
const containerWidth = computed(() => {
  const [width, height] = containerAspectRatio.value;
  return `calc(100vh * ${width} / ${height})`;
});

// 视频格子数据
const videoCells = ref<VideoCell[]>([]);
const currentVideos = shallowRef<HTMLVideoElement[]>([]);
const nextVideos = shallowRef<HTMLVideoElement[]>([]);
const preloadLock = ref(false); // 预加载锁防止重复请求
const schedulePreloadTimer = ref<NodeJS.Timeout | null>(null); // 预加载防抖定时器

// 视频缓存系统
const videoCache = ref<Map<string, VideoCacheItem>>(new Map());
const MAX_CACHE_SIZE = 300 * 1024 * 1024; // 300MB内存缓存
let currentCacheSize = 0;
const indexedDBName = "videoCacheDB";
const indexedDBStoreName = "videos";

// 初始化IndexedDB
async function initIndexedDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (!("indexedDB" in window)) {
      reject(new Error("IndexedDB not supported"));
      return;
    }

    const request = indexedDB.open(indexedDBName, 1);

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      
      // 视频缓存存储
      if (!db.objectStoreNames.contains(indexedDBStoreName)) {
        const store = db.createObjectStore(indexedDBStoreName, {
          keyPath: "id",
        });
        store.createIndex("lastUsed", "lastUsed", { unique: false });
        store.createIndex("size", "size", { unique: false });
      }
    };

    request.onsuccess = (event) => {
      resolve((event.target as IDBOpenDBRequest).result);
    };

    request.onerror = (event) => {
      reject((event.target as IDBOpenDBRequest).error);
    };
  });
}

// 加载IndexedDB缓存到内存
async function loadCacheFromIndexedDB() {
  try {
    const db = await initIndexedDB();
    
    // 加载视频缓存
    const videoTransaction = db.transaction(indexedDBStoreName, "readonly");
    const videoStore = videoTransaction.objectStore(indexedDBStoreName);
    const videoRequest = videoStore.getAll();

    await new Promise<void>((resolve, reject) => {
      videoRequest.onsuccess = () => {
        let validCacheCount = 0;
        videoRequest.result.forEach((item: VideoCacheItem) => {
          // 页面刷新后，所有blob URL都会失效，需要清理
          if (item.blobUrl && item.blobUrl.startsWith("blob:")) {
            console.warn(`清理失效的视频Blob URL缓存: ${item.id}`);
            // 不加载失效的blob URL缓存
            return;
          }

          if (!videoCache.value.has(item.id)) {
            videoCache.value.set(item.id, item);
            currentCacheSize += item.size;
            validCacheCount++;
          }
        });
        console.log(`从IndexedDB加载了 ${validCacheCount} 个有效视频缓存项`);
        resolve();
      };

      videoRequest.onerror = () => {
        reject(videoRequest.error);
      };
    });

    // 初始缓存清理
    cleanUpCache();
  } catch (error) {
    console.error("加载IndexedDB缓存失败:", error);
  }
}

// 保存缓存到IndexedDB
async function saveCacheToIndexedDB(item: VideoCacheItem) {
  try {
    const db = await initIndexedDB();
    const transaction = db.transaction(indexedDBStoreName, "readwrite");
    const store = transaction.objectStore(indexedDBStoreName);
    store.put(item);
  } catch (error) {
    console.error("保存到IndexedDB失败:", error);
  }
}

// 清理缓存（LRU策略）
async function cleanUpCache() {
  // 收集正在使用的视频ID
  const inUseIds = new Set<string>();
  videoCells.value.forEach((cell) => {
    if (cell.current?.id) inUseIds.add(cell.current.id);
    if (cell.next?.id) inUseIds.add(cell.next.id);
  });

  // 清理内存缓存（跳过正在使用的）
  if (currentCacheSize > MAX_CACHE_SIZE) {
    const sortedEntries = [...videoCache.value.entries()].sort(
      (a, b) => a[1].lastUsed - b[1].lastUsed
    );

    for (const [id, item] of sortedEntries) {
      if (currentCacheSize <= MAX_CACHE_SIZE * 0.7) break;
      if (inUseIds.has(id)) continue; // 跳过正在使用的

      // 释放Blob URL
      if (item.blobUrl) {
        URL.revokeObjectURL(item.blobUrl);
      }

      videoCache.value.delete(id);
      currentCacheSize -= item.size;

      // 从IndexedDB中移除
      try {
        const db = await initIndexedDB();
        const transaction = db.transaction(indexedDBStoreName, "readwrite");
        const store = transaction.objectStore(indexedDBStoreName);
        store.delete(id);
      } catch (error) {
        console.error("清理IndexedDB缓存失败:", error);
      }
    }
  }

  // 清理IndexedDB中过期的缓存（每周清理一次）
  const lastClean = localStorage.getItem("lastCacheClean") || "0";
  const now = Date.now();

  if (now - Number.parseInt(lastClean) > 7 * 24 * 3600 * 1000) {
    localStorage.setItem("lastCacheClean", now.toString());

    try {
      const db = await initIndexedDB();
      const transaction = db.transaction(indexedDBStoreName, "readwrite");
      const store = transaction.objectStore(indexedDBStoreName);
      const index = store.index("lastUsed");
      const cutoff = now - 30 * 24 * 3600 * 1000; // 删除30天未使用的

      const request = index.openCursor(IDBKeyRange.upperBound(cutoff));

      request.onsuccess = (event) => {
        const cursor = (event.target as IDBRequest).result;
        if (cursor) {
          // 更新内存缓存大小
          const item = cursor.value;
          if (videoCache.value.has(item.id)) {
            videoCache.value.delete(item.id);
            currentCacheSize -= item.size;
          }

          cursor.delete();
          cursor.continue();
        }
      };
    } catch (error) {
      console.error("清理过期缓存失败:", error);
    }
  }
}

// 获取缓存视频
function getCachedVideo(id: string): VideoCacheItem | null {
  if (!id) return null;

  const cached = videoCache.value.get(id);
  if (cached) {
    // 检查Blob URL是否还有效（页面刷新后会失效）
    if (cached.blobUrl && !cached.blobUrl.startsWith("blob:")) {
      // 如果不是blob URL，说明是原始URL，可以使用
      cached.lastUsed = Date.now();
      return cached;
    } else if (cached.blobUrl && cached.blobUrl.startsWith("blob:")) {
      // 如果是blob URL，需要验证是否还有效
      // 页面刷新后blob URL会失效，需要重新创建
      console.warn(`Blob URL可能已失效: ${id}，将重新加载`);
      // 清理失效的缓存项
      videoCache.value.delete(id);
      return null;
    }
  }
  return null;
}

// 缓存视频
async function cacheVideo(item: VideoCacheItem) {
  if (!item.id || !item.blobUrl) return;

  // 更新最后使用时间
  item.lastUsed = Date.now();

  // 如果已存在缓存，先清理旧版本
  if (videoCache.value.has(item.id)) {
    const existing = videoCache.value.get(item.id)!;
    // URL.revokeObjectURL(existing.blobUrl);
    currentCacheSize -= existing.size;
  }

  // 添加到内存缓存
  videoCache.value.set(item.id, item);
  currentCacheSize += item.size;

  // 保存到IndexedDB
  await saveCacheToIndexedDB(item);

  // 清理缓存
  cleanUpCache();
}

// 清理失效的IndexedDB缓存
async function cleanInvalidCacheFromIndexedDB() {
  try {
    const db = await initIndexedDB();
    
    // 清理视频缓存
    const videoTransaction = db.transaction(indexedDBStoreName, "readwrite");
    const videoStore = videoTransaction.objectStore(indexedDBStoreName);
    const videoRequest = videoStore.getAll();

    await new Promise<void>((resolve, reject) => {
      videoRequest.onsuccess = () => {
        const deletePromises: Promise<void>[] = [];

        videoRequest.result.forEach((item: VideoCacheItem) => {
          // 删除所有blob URL缓存，因为页面刷新后都会失效
          if (item.blobUrl && item.blobUrl.startsWith("blob:")) {
            const deleteRequest = videoStore.delete(item.id);
            deletePromises.push(
              new Promise((resolve) => {
                deleteRequest.onsuccess = () => resolve();
                deleteRequest.onerror = () => resolve(); // 即使删除失败也继续
              })
            );
          }
        });

        Promise.all(deletePromises).then(() => {
          console.log(`清理了 ${deletePromises.length} 个失效的视频blob URL缓存`);
          resolve();
        });
      };

      videoRequest.onerror = () => {
        reject(videoRequest.error);
      };
    });
  } catch (error) {
    console.error("清理失效缓存失败:", error);
  }
}

// 初始化视频格子
async function initVideoCells() {
  console.log("开始初始化视频格子");

  // 获取容器宽高比配置
  containerAspectRatio.value = await fetchContainerAspectRatio();

  const total = gridSize.value * gridSize.value;
  videoCells.value = new Array(total).fill(null).map(() => ({
    current: null,
    next: null,
    transitioning: false,
  }));

  // 清理失效的缓存
  await cleanInvalidCacheFromIndexedDB();

  // 加载有效缓存
  await loadCacheFromIndexedDB();

  // 批量加载初始视频
  await preloadInitialVideos();

  console.log("视频格子初始化完成");

  // 延迟预加载下一批视频，避免初始化时的请求冲突
  setTimeout(() => {
    if (!preloadLock.value) {
      schedulePreload();
    }
  }, 1000); // 减少延迟时间，更快开始预加载
}

// 预加载初始视频
async function preloadInitialVideos() {
  console.log("开始预加载初始视频");
  const total = gridSize.value * gridSize.value;
  const videos = await castingService.getCurrentCasting("VIDEO_EFFECT", total);

  console.log(`获取到 ${videos.length} 个视频，需要 ${total} 个`);

  // 兼容接口返回数量不足的情况
  const actualVideoCount = Math.min(videos.length, total);

  await Promise.all(
    videos.slice(0, actualVideoCount).map(async (video, index) => {
      console.log(`处理视频 ${index}: ${video.id}`);

      // 优先使用缓存
      const cached = getCachedVideo(video.id);
      if (cached) {
        console.log(`使用缓存视频 ${index}: ${video.id}`);
        videoCells.value[index].current = {
          ...video,
          ready: true,
          blobUrl: cached.url || cached.blobUrl, // 优先使用原始URL
          startTime: undefined,
          hasPlayed: false,
        };
        return;
      }

      // 否则加载新视频
      console.log(`预加载新视频 ${index}: ${video.id}`);
      const videoItem: VideoItem = {
        ...video,
        ready: false,
        startTime: undefined,
        hasPlayed: false,
      };
      videoCells.value[index].current = videoItem;
      await preloadVideo(videoItem);
    })
  );

  // 如果接口返回的视频数量不足，用缓存中的视频填充剩余格子
  if (actualVideoCount < total) {
    console.log(
      `视频数量不足，用缓存填充剩余 ${total - actualVideoCount} 个格子`
    );
    const remainingSlots = total - actualVideoCount;
    const cachedVideos = Array.from(videoCache.value.values());

    for (let i = 0; i < remainingSlots && i < cachedVideos.length; i++) {
      const slotIndex = actualVideoCount + i;
      const cachedVideo = cachedVideos[i];

      console.log(`填充格子 ${slotIndex} 使用缓存视频: ${cachedVideo.id}`);

      videoCells.value[slotIndex].current = {
        id: cachedVideo.id,
        url: cachedVideo.url || cachedVideo.blobUrl, // 优先使用原始URL
        poster: cachedVideo.poster,
        ready: true,
        blobUrl: cachedVideo.url || cachedVideo.blobUrl,
        startTime: undefined,
        hasPlayed: false,
      };
    }
  }

  console.log("初始视频预加载完成");
}

// 预加载单个视频
async function preloadVideo(videoItem: VideoItem): Promise<void> {
  return new Promise(async (resolve, reject) => {
    try {
      // 检查缓存
      const cached = getCachedVideo(videoItem.id);
      if (cached) {
        videoItem.ready = true;
        videoItem.blobUrl = cached.blobUrl;
        resolve();
        return;
      }

      console.log(`开始预加载视频: ${videoItem.id}`);

      // 从网络加载
      const response = await fetch(videoItem.url);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      // 获取视频大小
      const contentLength = response.headers.get("Content-Length");
      const size = contentLength ? Number.parseInt(contentLength) : 0;

      // 转换为Blob
      const blob = await response.blob();
      const blobUrl = URL.createObjectURL(blob);

      // 创建缓存项
      const cacheItem: VideoCacheItem = {
        id: videoItem.id,
        blobUrl,
        poster: videoItem.poster,
        lastUsed: Date.now(),
        size,
        url: videoItem.url, // 保存原始URL
      };

      // 保存到缓存
      await cacheVideo(cacheItem);

      // 更新视频项
      videoItem.ready = true;
      videoItem.blobUrl = blobUrl;
      console.log(`视频预加载完成: ${videoItem.id}`);
      resolve();
    } catch (error) {
      console.error(`视频预加载失败: ${videoItem.url}`, error);

      // 即使预加载失败，也可以尝试直接使用原始URL
      videoItem.ready = true;
      videoItem.blobUrl = videoItem.url;
      console.log(`视频预加载失败，将使用原始URL: ${videoItem.id}`);
      resolve(); // 不reject，让视频尝试直接播放原始URL
    }
  });
}

// 预加载下一批视频（带防抖）
function schedulePreload() {
  // 清除之前的定时器
  if (schedulePreloadTimer.value) {
    clearTimeout(schedulePreloadTimer.value);
  }

  // 防抖：延迟执行，避免频繁调用
  schedulePreloadTimer.value = setTimeout(() => {
    doPreload();
  }, 200); // 减少防抖延迟，更快响应预加载需求
}

// 实际执行预加载的函数
function doPreload() {
  if (preloadLock.value) return;

  // 计算需要预加载的数量，优先考虑即将播放完的视频
  const emptySlots = videoCells.value.filter((cell) => !cell.next).length;
  if (emptySlots === 0) return;

  // 按优先级排序：即将播放完的视频优先预加载
  const prioritizedSlots = getPrioritizedEmptySlots();
  if (prioritizedSlots.length === 0) return;

  preloadLock.value = true;
  const preloadCount = Math.min(prioritizedSlots.length, gridSize.value * gridSize.value);

  castingService
    .getCurrentCasting("VIDEO_EFFECT", preloadCount)
    .then(async (videos) => {
      const preloadTasks: Promise<void>[] = [];
      const actualVideos = videos || []; // 防止接口返回null/undefined

      for (let i = 0; i < Math.min(actualVideos.length, prioritizedSlots.length); i++) {
        const cellIndex = prioritizedSlots[i];

        // 优先使用缓存
        const cached = getCachedVideo(actualVideos[i].id);
        if (cached) {
          videoCells.value[cellIndex].next = {
            ...actualVideos[i],
            ready: true,
            blobUrl: cached.url || cached.blobUrl,
            startTime: undefined,
            hasPlayed: false,
          };
          continue;
        }

        // 否则创建新视频项
        const videoItem: VideoItem = {
          ...actualVideos[i],
          ready: false,
          startTime: undefined,
          hasPlayed: false,
        };
        videoCells.value[cellIndex].next = videoItem;
        preloadTasks.push(preloadVideo(videoItem));
      }

      // 如果接口返回的视频不足，尝试从缓存中补充
      if (actualVideos.length < prioritizedSlots.length) {
        const remainingSlots = prioritizedSlots.slice(actualVideos.length);
        const cachedVideos = Array.from(videoCache.value.values());
        const currentIds = videoCells.value
          .filter((cell) => cell.current || cell.next)
          .map((cell) => [cell.current?.id, cell.next?.id])
          .flat()
          .filter(Boolean);

        const availableCached = cachedVideos.filter(
          (cached) => !currentIds.includes(cached.id)
        );

        for (let i = 0; i < Math.min(remainingSlots.length, availableCached.length); i++) {
          const cellIndex = remainingSlots[i];
          const cachedVideo = availableCached[i];
          
          videoCells.value[cellIndex].next = {
            id: cachedVideo.id,
            url: cachedVideo.url || cachedVideo.blobUrl,
            poster: cachedVideo.poster,
            ready: true,
            blobUrl: cachedVideo.url || cachedVideo.blobUrl,
            startTime: undefined,
            hasPlayed: false,
          };
        }
      }

      await Promise.all(preloadTasks);
      console.log(
        `预加载完成: ${preloadTasks.length}个新视频，${actualVideos.length}个总视频`
      );
    })
    .catch((error) => {
      console.error("预加载请求失败:", error);
      // 降低重试频率，避免疯狂请求
      setTimeout(() => {
        if (!preloadLock.value) {
          // 确保没有其他预加载在进行
          schedulePreload();
        }
      }, 3000); // 减少重试延迟
    })
    .finally(() => {
      preloadLock.value = false;
    });
}

// 获取按优先级排序的空槽位
function getPrioritizedEmptySlots(): number[] {
  const emptySlots: { index: number; priority: number }[] = [];
  
  videoCells.value.forEach((cell, index) => {
    if (!cell.next) {
      let priority = 0;
      
      // 如果当前视频正在播放，计算播放进度
      const videoEl = currentVideos.value.find(
        (el) => Number.parseInt(el.dataset.index || "0") === index
      );
      
      if (videoEl && videoEl.duration && cell.current) {
        const progress = videoEl.currentTime / videoEl.duration;
        // 播放进度越高，优先级越高
        priority = progress * 100;
      }
      
      emptySlots.push({ index, priority });
    }
  });
  
  // 按优先级降序排序
  return emptySlots
    .sort((a, b) => b.priority - a.priority)
    .map(slot => slot.index);
}



// 获取有效的视频源URL
function getValidVideoSrc(videoItem: VideoItem): string {
  // 优先使用原始URL，确保视频能够播放
  if (videoItem.url) {
    return videoItem.url;
  }

  // 如果没有原始URL，尝试使用blobUrl（虽然可能失效）
  if (videoItem.blobUrl) {
    return videoItem.blobUrl;
  }

  // 最后的回退
  return "";
}

// 添加辅助函数：从缓存中随机获取一个视频
function getRandomCachedVideo(): VideoCacheItem | null {
  if (videoCache.value.size === 0) return null;

  const cachedVideos = Array.from(videoCache.value.values());
  // 过滤掉当前正在播放的视频和失效的blob URL
  const currentIds = videoCells.value
    .filter((cell) => cell.current)
    .map((cell) => cell.current!.id);

  const availableVideos = cachedVideos.filter(
    (video) => !currentIds.includes(video.id)
  );

  // 优先选择非当前播放的视频，如果没有则使用所有缓存视频
  const pool = availableVideos.length > 0 ? availableVideos : cachedVideos;

  if (pool.length === 0) return null;

  const randomIndex = Math.floor(Math.random() * pool.length);
  return pool[randomIndex];
}

// 处理视频播放结束
function onVideoEnded(index: number) {
  const cell = videoCells.value[index];
  const videoEl = currentVideos.value.find(
    (el) => Number.parseInt(el.dataset.index || "0") === index
  );

  if (!cell || !videoEl) return;

  const currentVideo = cell.current;
  if (!currentVideo) return;

  // 检查视频是否真正播放过（防止刚加载就触发ended）
  const now = Date.now();
  const minPlayTime = 3000; // 最少播放3秒

  // 如果视频刚开始播放不久就结束，可能是异常情况
  if (currentVideo.startTime && now - currentVideo.startTime < minPlayTime) {
    console.warn(
      `视频 ${index} 播放时间过短 (${now - currentVideo.startTime}ms)，重新播放`
    );
    videoEl.currentTime = 0;
    currentVideo.startTime = now; // 重置开始时间
    safePlay(videoEl);
    return;
  }

  // 检查视频播放进度，确保是正常结束
  if (videoEl.duration && videoEl.currentTime < videoEl.duration * 0.9) {
    console.warn(
      `视频 ${index} 未播放完整 (${videoEl.currentTime}/${videoEl.duration})，重新播放`
    );
    videoEl.currentTime = 0;
    currentVideo.startTime = now;
    safePlay(videoEl);
    return;
  }

  // 标记视频已正常播放完成
  currentVideo.hasPlayed = true;
  console.log(`视频 ${index} 正常播放完成，准备切换`);

  // 尝试无缝切换到下一个视频
  attemptSeamlessTransition(index);
}

// 尝试无缝切换到下一个视频
function attemptSeamlessTransition(index: number) {
  const cell = videoCells.value[index];
  if (!cell) return;

  if (cell.next?.ready) {
    // 下一个视频已准备好，执行无缝切换
    performSeamlessTransition(index);
  } else {
    // 下一个视频未准备好，循环播放当前视频
    console.log(`视频 ${index} 下一个视频未准备好，循环播放当前视频`);
    loopCurrentVideo(index);
    
    // 触发预加载，确保尽快准备下一个视频
    if (!preloadLock.value) {
      schedulePreload();
    }
    
    // 定期检查下一个视频是否准备好
    checkNextVideoReady(index);
  }
}

// 循环播放当前视频
function loopCurrentVideo(index: number) {
  const cell = videoCells.value[index];
  const videoEl = currentVideos.value.find(
    (el) => Number.parseInt(el.dataset.index || "0") === index
  );

  if (!cell?.current || !videoEl) return;

  // 尝试从缓存中获取一个不同的视频来避免单调
  const randomCachedVideo = getRandomCachedVideo();
  
  if (randomCachedVideo && randomCachedVideo.id !== cell.current.id) {
    // 使用缓存中的不同视频
    const newVideoItem: VideoItem = {
      id: randomCachedVideo.id,
      url: randomCachedVideo.url || randomCachedVideo.blobUrl,
      poster: randomCachedVideo.poster,
      ready: true,
      blobUrl: randomCachedVideo.url || randomCachedVideo.blobUrl,
      startTime: Date.now(),
      hasPlayed: false,
    };

    cell.current = newVideoItem;
    
    nextTick(() => {
      if (videoEl) {
        videoEl.src = getValidVideoSrc(newVideoItem);
        videoEl.currentTime = 0;
        safePlay(videoEl);
      }
    });

    // 更新缓存使用时间
    randomCachedVideo.lastUsed = Date.now();
  } else {
    // 重新播放当前视频
    videoEl.currentTime = 0;
    cell.current.startTime = Date.now();
    cell.current.hasPlayed = false;
    safePlay(videoEl);
  }
}

// 定期检查下一个视频是否准备好
function checkNextVideoReady(index: number) {
  const checkInterval = setInterval(() => {
    const cell = videoCells.value[index];
    
    if (!cell) {
      clearInterval(checkInterval);
      return;
    }

    if (cell.next?.ready) {
      // 下一个视频准备好了，执行无缝切换
      console.log(`视频 ${index} 下一个视频已准备好，执行切换`);
      clearInterval(checkInterval);
      performSeamlessTransition(index);
    }
  }, 1000); // 每秒检查一次

  // 最多检查30秒，避免无限检查
  setTimeout(() => {
    clearInterval(checkInterval);
  }, 30000);
}

// 执行无缝切换
function performSeamlessTransition(index: number) {
  const cell = videoCells.value[index];
  if (!cell?.next?.ready) return;

  // 使用淡入淡出效果实现无缝切换
  cell.transitioning = true;
  
  // 立即开始切换动画
  nextTick(() => {
    // 预加载下一个视频到播放状态
    const nextVideoEl = nextVideos.value.find(
      (el) => Number.parseInt(el.dataset.index || "0") === index
    );
    
    if (nextVideoEl && cell.next) {
      nextVideoEl.currentTime = 0;
      cell.next.startTime = Date.now();
      cell.next.hasPlayed = false;
      
      // 开始播放下一个视频（静音状态）
      safePlay(nextVideoEl).then(() => {
        // 视频开始播放后，触发切换动画
        setTimeout(() => {
          onTransitionEnd(index);
        }, 300); // 300ms 后完成切换
      });
    }
  });
}

// 过渡动画结束
function onTransitionEnd(index: number) {
  const cell = videoCells.value[index];
  if (!cell) return;

  cell.transitioning = false;

  if (cell.next?.ready) {
    // 清理当前视频资源
    if (cell.current?.blobUrl && cell.current.blobUrl.startsWith("blob:")) {
      URL.revokeObjectURL(cell.current.blobUrl);
    }

    // 切换到预加载的视频
    const nextVideo = cell.next;
    const currentVideoEl = currentVideos.value.find(
      (el) => Number.parseInt(el.dataset.index || "0") === index
    );
    const nextVideoEl = nextVideos.value.find(
      (el) => Number.parseInt(el.dataset.index || "0") === index
    );

    // 更新视频源到当前播放元素
    if (currentVideoEl && nextVideo) {
      currentVideoEl.src = getValidVideoSrc(nextVideo);
      currentVideoEl.currentTime = 0;
      
      // 如果下一个视频元素已经在播放，同步时间
      if (nextVideoEl && !nextVideoEl.paused) {
        currentVideoEl.currentTime = nextVideoEl.currentTime;
      }
    }

    // 更新状态
    cell.current = nextVideo;
    cell.next = null;

    // 设置新视频的开始时间
    if (nextVideo) {
      nextVideo.startTime = Date.now();
      nextVideo.hasPlayed = false;
    }

    // 确保新视频播放
    nextTick(() => {
      if (currentVideoEl && nextVideo) {
        console.log(`无缝切换到新视频 ${index}`);
        safePlay(currentVideoEl).catch(() => {
          console.error(`切换后的视频播放失败: ${index}`);
        });
      }
    });

    // 立即触发新的预加载，确保下一个视频尽快准备
    setTimeout(() => {
      if (!preloadLock.value) {
        schedulePreload();
      }
    }, 500); // 减少延迟，更快预加载
  }
}

// 视频开始加载时的处理
function onVideoLoadStart(index: number) {
  const cell = videoCells.value[index];
  if (cell?.current) {
    console.log(`视频 ${index} 开始加载`);
    cell.current.startTime = undefined; // 重置开始时间
    cell.current.hasPlayed = false;
  }
}

// 视频时间更新处理
function onVideoTimeUpdate(index: number) {
  const videoEl = currentVideos.value.find(
    (el) => Number.parseInt(el.dataset.index || "0") === index
  );
  const cell = videoCells.value[index];

  if (videoEl && cell?.current && !cell.current.hasPlayed) {
    // 当视频播放超过5秒时，标记为已正常播放
    if (videoEl.currentTime > 5) {
      cell.current.hasPlayed = true;
      console.log(
        `视频 ${index} 已正常播放 ${videoEl.currentTime.toFixed(1)}s`
      );
    }
  }

  // 预加载优化：当视频播放到70%时，确保下一个视频已经准备好
  if (videoEl && videoEl.duration && cell?.current) {
    const progress = videoEl.currentTime / videoEl.duration;
    
    if (progress > 0.7 && !cell.next?.ready) {
      // 视频播放到70%但下一个视频还没准备好，触发预加载
      if (!preloadLock.value) {
        console.log(`视频 ${index} 播放到70%，触发预加载`);
        schedulePreload();
      }
    }
    
    // 当播放到90%时，如果下一个视频还没准备好，准备循环当前视频
    if (progress > 0.9 && !cell.next?.ready) {
      console.log(`视频 ${index} 播放到90%，下一个视频仍未准备好`);
      // 可以在这里做一些准备工作，比如预先准备循环播放
    }
  }
}

// 视频可以播放时的处理
function handleVideoCanPlay(index: number) {
  const videoEl = currentVideos.value.find(
    (el) => Number.parseInt(el.dataset.index || "0") === index
  );
  const cell = videoCells.value[index];

  console.log(`视频 ${index} 可以播放，src: ${videoEl?.src}`);

  if (videoEl && videoEl.paused && cell?.current) {
    // 记录视频开始播放时间
    cell.current.startTime = Date.now();
    cell.current.hasPlayed = false;
    console.log(`视频 ${index} 开始播放，URL: ${videoEl.src}`);
    safePlay(videoEl).catch(() => {
      console.error(`视频 ${index} canplay后播放失败`);
    });
  }
}

// 重新加载单个视频
async function reloadVideo(index: number) {
  const cell = videoCells.value[index];
  if (!cell) return;

  // 防止频繁重新加载
  const now = Date.now();
  const lastReloadTime = cell.lastReloadTime || 0;
  if (now - lastReloadTime < 15000) {
    // 15秒内不重复加载
    console.warn(`视频 ${index} 重新加载过于频繁，跳过`);
    return;
  }

  cell.lastReloadTime = now;

  try {
    console.log(`开始重新加载视频 ${index}`);

    // 先尝试从缓存中获取不同的视频
    const randomCachedVideo = getRandomCachedVideo();
    if (randomCachedVideo) {
      console.log(`使用缓存视频替换 ${index}`);

      // 使用原始URL而不是可能失效的blob URL
      const videoSrc = randomCachedVideo.url || randomCachedVideo.blobUrl;

      cell.current = {
        id: randomCachedVideo.id,
        url: videoSrc,
        poster: randomCachedVideo.poster,
        ready: true,
        blobUrl: videoSrc,
        startTime: now,
        hasPlayed: false,
      };

      nextTick(() => {
        const videoEl = currentVideos.value.find(
          (el) => Number.parseInt(el.dataset.index || "0") === index
        );
        if (videoEl) {
          videoEl.src = videoSrc;
          videoEl.currentTime = 0;
          safePlay(videoEl).catch(() => {
            console.error(`缓存视频播放失败: ${index}`);
          });
        }
      });
      return;
    }

    // 如果缓存中没有可用视频，才从网络加载
    const [video] = await castingService.getCurrentCasting("VIDEO_EFFECT", 1);

    // 优先使用缓存
    const cached = getCachedVideo(video.id);
    if (cached) {
      cell.current = {
        ...video,
        ready: true,
        blobUrl: cached.blobUrl,
        startTime: now,
        hasPlayed: false,
      };
    } else {
      // 否则加载新视频
      const videoItem: VideoItem = {
        ...video,
        ready: false,
        startTime: now,
        hasPlayed: false,
      };
      cell.current = videoItem;
      await preloadVideo(videoItem);
    }

    // 确保播放
    nextTick(() => {
      const videoEl = currentVideos.value.find(
        (el) => Number.parseInt(el.dataset.index || "0") === index
      );
      if (videoEl && cell.current) {
        cell.current.startTime = Date.now();
        safePlay(videoEl).catch(() => {
          console.error(`重新加载的视频播放失败: ${index}`);
        });
      }
    });
  } catch (e) {
    console.error(`加载视频失败: ${index}`, e);
    // 不再自动重试，避免无限循环
  }
}

// 处理视频错误
function handleVideoError(index: number) {
  const cell = videoCells.value[index];
  if (!cell?.current) return;

  const now = Date.now();
  const lastErrorTime = cell.current.lastErrorTime || 0;

  // 防止频繁错误重试，至少间隔10秒
  if (now - lastErrorTime < 10000) {
    console.warn(`视频 ${index} 错误过于频繁，跳过重试`);
    return;
  }

  cell.current.lastErrorTime = now;
  console.error(`视频播放错误: ${index}，尝试重新加载`);

  // 延迟重新加载，避免立即重试
  setTimeout(() => {
    reloadVideo(index);
  }, 2000);
}

// 安全播放函数
function safePlay(video: HTMLVideoElement): Promise<void> {
  return new Promise((resolve, reject) => {
    let retryCount = 0;
    const maxRetries = 3;

    const playAttempt = () => {
      video
        .play()
        .then(() => {
          console.log(`视频播放成功: ${video.dataset.index}`);
          resolve();
        })
        .catch((error) => {
          console.error(
            `视频播放失败 (尝试 ${retryCount + 1}/${maxRetries}):`,
            error
          );
          retryCount++;

          if (error.name === "AbortError" || error.name === "NotAllowedError") {
            if (retryCount < maxRetries) {
              setTimeout(playAttempt, 1000 * retryCount); // 递增延迟
            } else {
              console.error("视频播放权限被拒绝，停止重试");
              reject(error);
            }
          } else if (retryCount < maxRetries) {
            // 其他错误，限制重试次数
            setTimeout(playAttempt, 1000 * retryCount);
          } else {
            console.error("视频播放失败，已达到最大重试次数");
            reject(error);
          }
        });
    };

    playAttempt();
  });
}

// 心跳检测 - 监控卡顿的视频
function startHeartbeat() {
  const intervalId = setInterval(() => {
    currentVideos.value.forEach((video) => {
      const index = Number.parseInt(video.dataset.index || "0");
      const cell = videoCells.value[index];

      if (!cell?.current) return;

      // 更严格的卡顿检测条件
      const now = Date.now();
      const videoStartTime = cell.current.startTime || 0;
      const hasBeenPlayingLongEnough = now - videoStartTime > 10000; // 播放超过10秒

      const isStuck =
        video.readyState > 0 &&
        video.paused &&
        !video.seeking &&
        !video.ended &&
        cell.current.ready &&
        video.currentTime > 5 && // 确保视频已经播放了一段时间
        hasBeenPlayingLongEnough; // 确保不是刚开始播放

      if (isStuck) {
        console.warn(
          `检测到长时间卡顿的视频 ${index}，播放时长: ${video.currentTime}s`
        );

        // 记录恢复尝试，避免频繁恢复
        if (
          !cell.current.lastRecoveryAttempt ||
          now - cell.current.lastRecoveryAttempt > 30000
        ) {
          cell.current.lastRecoveryAttempt = now;

          safePlay(video).catch(() => {
            console.warn(`视频 ${index} 恢复播放失败`);
            // 不再自动重新加载，避免无限循环
          });
        }
      }
    });
  }, 30000); // 降低到30秒检测一次，减少干扰

  return intervalId;
}

let heartbeatInterval: number | null = null;

onMounted(() => {
  console.log("视频页面初始化开始");
  initVideoCells();
  heartbeatInterval = startHeartbeat() as unknown as number;
  console.log("视频页面初始化完成");
});

// 清理
onUnmounted(() => {
  // 停止心跳检测
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval);
  }

  // 清理预加载定时器
  if (schedulePreloadTimer.value) {
    clearTimeout(schedulePreloadTimer.value);
  }

  // 不再清理缓存中的Blob URL，只清理当前单元格
  videoCells.value.forEach((cell) => {
    if (cell.current?.blobUrl && cell.current.blobUrl.startsWith("blob:")) {
      URL.revokeObjectURL(cell.current.blobUrl);
    }
    if (cell.next?.blobUrl && cell.next.blobUrl.startsWith("blob:")) {
      URL.revokeObjectURL(cell.next.blobUrl);
    }
  });

  
});
</script>

<template>
  <div class="video-wall-container">
    <LogoutButton :transparent="true" />
    <div class="video-grid">
      <div
        v-for="(cell, index) in videoCells"
        :key="`cell-${index}-${cell.current?.id || 'empty'}`"
        class="video-cell"
      >
        <!-- 当前播放的视频 -->
        <video
          v-if="cell.current && cell.current.ready"
          ref="currentVideos"
          :data-index="index"
          :src="getValidVideoSrc(cell.current)"
          :poster="cell.current.poster"
          autoplay
          muted
          playsinline
          :class="['video-element', 'current', { 'transitioning-out': cell.transitioning }]"
          @ended="onVideoEnded(index)"
          @error="handleVideoError(index)"
          @canplay="handleVideoCanPlay(index)"
          @timeupdate="onVideoTimeUpdate(index)"
          @loadstart="onVideoLoadStart(index)"
        />

        <!-- 预加载的下一个视频（已加载完成但隐藏） -->
        <video
          v-if="cell.next && cell.next.ready"
          ref="nextVideos"
          :data-index="index"
          :src="getValidVideoSrc(cell.next)"
          muted
          playsinline
          preload="auto"
          :class="['video-element', 'next', { 'transitioning-in': cell.transitioning }]"
        />

        <!-- 无缝切换遮罩 -->
        <div
          v-if="cell.transitioning"
          class="seamless-transition-overlay"
          @animationend="onTransitionEnd(index)"
        />

        <!-- 加载指示器 -->
        <img
          v-if="!cell.current?.ready && cell.current?.poster"
          class="h-full w-full object-cover"
          :src="cell.current.poster"
          alt=""
        />

        <!-- <div v-if="!cell.current?.ready" class="loading-indicator">
          <div class="spinner"></div>
        </div> -->
      </div>
    </div>
  </div>
</template>

<style scoped>
.video-wall-container {
  height: 100vh;
  width: v-bind("containerWidth");
  margin: 0 auto;
  overflow: hidden;
  background-color: #000;
  position: relative;
  display: flex;
  flex-direction: column;
}

.video-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(v-bind("gridSize"), 1fr);
  grid-template-rows: repeat(v-bind("gridSize"), 1fr);
  width: 100%;
  height: 100%;
  gap: 0;
  box-sizing: border-box;
}

.video-cell {
  position: relative;
  background-color: #111;
  overflow: hidden;
  border-radius: 0;
}

.video-element {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 0.5s ease-in-out;
}

.video-element.current {
  opacity: 1;
  z-index: 2;
}

.video-element.current.transitioning-out {
  opacity: 0;
  z-index: 1;
}

.video-element.next {
  opacity: 0;
  z-index: 1;
}

.video-element.next.transitioning-in {
  opacity: 1;
  z-index: 2;
}

.seamless-transition-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent;
  z-index: 3;
  pointer-events: none;
  animation: seamlessTransition 0.5s ease-in-out;
}

@keyframes seamlessTransition {
  0% {
    opacity: 0;
  }
  50% {
    opacity: 0;
  }
  100% {
    opacity: 0;
  }
}

.loading-indicator {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 5;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #fff;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-cell {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%);
  z-index: 1;
}

.empty-content {
  text-align: center;
  color: #666;
}

.empty-icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  opacity: 0.6;
}

.empty-text {
  font-size: 0.875rem;
  opacity: 0.8;
}
</style>
