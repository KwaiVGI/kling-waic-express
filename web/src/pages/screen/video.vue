<template>
  <div class="video-wall-container">
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
          :src="cell.current.blobUrl || cell.current.url"
          :poster="cell.current.poster"
          autoplay
          muted
          playsinline
          class="video-element active"
          @ended="onVideoEnded(index)"
          @error="handleVideoError(index)"
          @canplay="handleVideoCanPlay(index)"
        ></video>

        <!-- 预加载的下一个视频（已加载完成但隐藏） -->
        <video
          v-if="cell.next && cell.next.ready"
          ref="nextVideos"
          :data-index="index"
          :src="cell.next.blobUrl || cell.next.url"
          muted
          playsinline
          class="video-element next"
        ></video>

        <!-- 过渡遮罩 -->
        <div
          v-if="cell.transitioning"
          class="transition-overlay"
          @animationend="onTransitionEnd(index)"
        ></div>

        <!-- 加载指示器 -->
        <img
          v-if="!cell.current?.ready && cell.current?.poster"
          class="w-full h-full"
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

<script setup lang="ts">
import { castingService } from "@/api/castingService";
import { ref, onMounted, onUnmounted, shallowRef, nextTick } from "vue";

// 视频缓存项接口
interface VideoCacheItem {
  id: string;
  blobUrl: string;
  poster: string;
  lastUsed: number; // 最后使用时间戳
  size: number; // 视频大小（字节）
}

// 视频项接口
interface VideoItem {
  id: string;
  url: string;
  poster: string;
  ready: boolean;
  blobUrl?: string; // 当前使用的Blob URL
}

// 视频格子接口
interface VideoCell {
  current: VideoItem | null;
  next: VideoItem | null;
  transitioning: boolean;
}

// 配置参数
const gridSize = ref(3); // 默认3x3网格

// 视频格子数据
const videoCells = ref<VideoCell[]>([]);
const currentVideos = shallowRef<HTMLVideoElement[]>([]);
const nextVideos = shallowRef<HTMLVideoElement[]>([]);
const preloadLock = ref(false); // 预加载锁防止重复请求

// 视频缓存系统
const videoCache = ref<Map<string, VideoCacheItem>>(new Map());
const MAX_CACHE_SIZE = 300 * 1024 * 1024; // 300MB内存缓存
let currentCacheSize = 0;
const indexedDBName = "videoCacheDB";
const indexedDBStoreName = "videos";

// 初始化IndexedDB
const initIndexedDB = async (): Promise<IDBDatabase> => {
  return new Promise((resolve, reject) => {
    if (!("indexedDB" in window)) {
      reject(new Error("IndexedDB not supported"));
      return;
    }

    const request = indexedDB.open(indexedDBName, 1);

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
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
};

// 加载IndexedDB缓存到内存
const loadCacheFromIndexedDB = async () => {
  try {
    const db = await initIndexedDB();
    const transaction = db.transaction(indexedDBStoreName, "readonly");
    const store = transaction.objectStore(indexedDBStoreName);
    const request = store.getAll();

    await new Promise<void>((resolve, reject) => {
      request.onsuccess = () => {
        request.result.forEach((item: VideoCacheItem) => {
          if (!videoCache.value.has(item.id)) {
            videoCache.value.set(item.id, item);
            currentCacheSize += item.size;
          }
        });
        resolve();
      };

      request.onerror = () => {
        reject(request.error);
      };
    });

    // 初始缓存清理
    cleanUpCache();
  } catch (error) {
    console.error("加载IndexedDB缓存失败:", error);
  }
};

// 保存缓存到IndexedDB
const saveCacheToIndexedDB = async (item: VideoCacheItem) => {
  try {
    const db = await initIndexedDB();
    const transaction = db.transaction(indexedDBStoreName, "readwrite");
    const store = transaction.objectStore(indexedDBStoreName);
    store.put(item);
  } catch (error) {
    console.error("保存到IndexedDB失败:", error);
  }
};

// 清理缓存（LRU策略）
const cleanUpCache = async () => {
  // 清理内存缓存
  if (currentCacheSize > MAX_CACHE_SIZE) {
    const sortedEntries = [...videoCache.value.entries()].sort(
      (a, b) => a[1].lastUsed - b[1].lastUsed
    );

    while (
      currentCacheSize > MAX_CACHE_SIZE * 0.7 &&
      sortedEntries.length > 0
    ) {
      const [id, item] = sortedEntries.shift()!;

      // 释放Blob URL
      if (item.blobUrl) {
        URL.revokeObjectURL(item.blobUrl);
      }

      // 从内存缓存中移除
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

  if (now - parseInt(lastClean) > 7 * 24 * 3600 * 1000) {
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
};

// 获取缓存视频
const getCachedVideo = (id: string): VideoCacheItem | null => {
  if (!id) return null;

  const cached = videoCache.value.get(id);
  if (cached) {
    // 更新最后使用时间
    cached.lastUsed = Date.now();
    return cached;
  }
  return null;
};

// 缓存视频
const cacheVideo = async (item: VideoCacheItem) => {
  if (!item.id || !item.blobUrl) return;

  // 更新最后使用时间
  item.lastUsed = Date.now();

  // 如果已存在缓存，先清理旧版本
  if (videoCache.value.has(item.id)) {
    const existing = videoCache.value.get(item.id)!;
    URL.revokeObjectURL(existing.blobUrl);
    currentCacheSize -= existing.size;
  }

  // 添加到内存缓存
  videoCache.value.set(item.id, item);
  currentCacheSize += item.size;

  // 保存到IndexedDB
  await saveCacheToIndexedDB(item);

  // 清理缓存
  cleanUpCache();
};

// 初始化视频格子
const initVideoCells = async () => {
  const total = gridSize.value * gridSize.value;
  videoCells.value = Array(total)
    .fill(null)
    .map(() => ({
      current: null,
      next: null,
      transitioning: false,
    }));

  // 加载缓存
  await loadCacheFromIndexedDB();

  // 批量加载初始视频
  await preloadInitialVideos();

  // 预加载下一批视频
  schedulePreload();
};

// 预加载初始视频
const preloadInitialVideos = async () => {
  const total = gridSize.value * gridSize.value;
  const videos = await castingService.getCurrentCasting("VIDEO_EFFECT", total);

  await Promise.all(
    videos.map(async (video, index) => {
      if (index < total) {
        // 优先使用缓存
        const cached = getCachedVideo(video.id);
        if (cached) {
          videoCells.value[index].current = {
            ...video,
            ready: true,
            blobUrl: cached.blobUrl,
          };
          return;
        }

        // 否则加载新视频
        const videoItem: VideoItem = { ...video, ready: false };
        videoCells.value[index].current = videoItem;
        await preloadVideo(videoItem);
      }
    })
  );
};

// 预加载单个视频
const preloadVideo = async (videoItem: VideoItem): Promise<void> => {
  return new Promise(async (resolve) => {
    try {
      // 检查缓存
      const cached = getCachedVideo(videoItem.id);
      if (cached) {
        videoItem.ready = true;
        videoItem.blobUrl = cached.blobUrl;
        resolve();
        return;
      }

      // 从网络加载
      const response = await fetch(videoItem.url);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      // 获取视频大小
      const contentLength = response.headers.get("Content-Length");
      const size = contentLength ? parseInt(contentLength) : 0;

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
      };

      // 保存到缓存
      await cacheVideo(cacheItem);

      // 更新视频项
      videoItem.ready = true;
      videoItem.blobUrl = blobUrl;
      resolve();
    } catch (error) {
      console.error(`视频加载失败: ${videoItem.url}`, error);

      // 重试机制
      setTimeout(async () => {
        console.log(`重试加载视频: ${videoItem.url}`);
        await preloadVideo(videoItem);
      }, 2000);
    }
  });
};

// 预加载下一批视频
const schedulePreload = () => {
  if (preloadLock.value) return;

  // 计算需要预加载的数量
  const emptySlots = videoCells.value.filter((cell) => !cell.next).length;
  if (emptySlots === 0) return;

  preloadLock.value = true;
  const preloadCount = Math.min(emptySlots, gridSize.value * gridSize.value);

  castingService
    .getCurrentCasting("VIDEO_EFFECT", preloadCount)
    .then(async (videos) => {
      const preloadTasks: Promise<void>[] = [];

      for (let i = 0; i < videos.length; i++) {
        const cellIndex = videoCells.value.findIndex((cell) => !cell.next);
        if (cellIndex === -1) break;

        // 优先使用缓存
        const cached = getCachedVideo(videos[i].id);
        if (cached) {
          videoCells.value[cellIndex].next = {
            ...videos[i],
            ready: true,
            blobUrl: cached.blobUrl,
          };
          continue;
        }

        // 否则创建新视频项
        const videoItem: VideoItem = { ...videos[i], ready: false };
        videoCells.value[cellIndex].next = videoItem;
        preloadTasks.push(preloadVideo(videoItem));
      }

      await Promise.all(preloadTasks);
      console.log(`预加载完成: ${preloadTasks.length}个视频`);
    })
    .catch((error) => {
      console.error("预加载请求失败:", error);
      // 重试机制
      setTimeout(schedulePreload, 3000);
    })
    .finally(() => {
      preloadLock.value = false;
    });
};

// 处理视频播放结束
const onVideoEnded = (index: number) => {
  const cell = videoCells.value[index];

  if (cell.next?.ready) {
    // 触发切换动画
    cell.transitioning = true;
  } else {
    // 没有准备好的下一个视频，重新播放当前视频
    const videoEl = currentVideos.value.find(
      (el) => parseInt(el.dataset.index || "0") === index
    );
    if (videoEl && cell.current?.ready) {
      videoEl.currentTime = 0;
      safePlay(videoEl);
    } else {
      // 当前视频也不可用，触发紧急加载
      reloadVideo(index);
    }

    // 触发预加载补充
    schedulePreload();
  }
};

// 过渡动画结束
const onTransitionEnd = (index: number) => {
  const cell = videoCells.value[index];
  if (!cell) return;

  cell.transitioning = false;

  if (cell.next?.ready) {
    // 清理当前视频资源
    if (cell.current?.blobUrl && !cell.current?.blobUrl.startsWith("blob:")) {
      URL.revokeObjectURL(cell.current.blobUrl);
    }

    // 切换到预加载的视频
    cell.current = cell.next;
    cell.next = null;

    // 确保新视频播放
    nextTick(() => {
      const videoEl = currentVideos.value.find(
        (el) => parseInt(el.dataset.index || "0") === index
      );
      if (videoEl) {
        safePlay(videoEl);
      }
    });

    // 触发新的预加载
    schedulePreload();
  }
};

// 视频可以播放时的处理
const handleVideoCanPlay = (index: number) => {
  const videoEl = currentVideos.value.find(
    (el) => parseInt(el.dataset.index || "0") === index
  );

  if (videoEl && videoEl.paused) {
    safePlay(videoEl);
  }
};

// 重新加载单个视频
const reloadVideo = async (index: number) => {
  try {
    const [video] = await castingService.getCurrentCasting("VIDEO_EFFECT", 1);
    const cell = videoCells.value[index];

    // 优先使用缓存
    const cached = getCachedVideo(video.id);
    if (cached) {
      cell.current = {
        ...video,
        ready: true,
        blobUrl: cached.blobUrl,
      };
      return;
    }

    // 否则加载新视频
    const videoItem: VideoItem = { ...video, ready: false };
    cell.current = videoItem;
    await preloadVideo(videoItem);

    // 确保播放
    nextTick(() => {
      const videoEl = currentVideos.value.find(
        (el) => parseInt(el.dataset.index || "0") === index
      );
      if (videoEl) {
        safePlay(videoEl);
      }
    });
  } catch (e) {
    console.error(`加载视频失败: ${index}`, e);
    // 重试机制
    setTimeout(() => reloadVideo(index), 3000);
  }
};

// 处理视频错误
const handleVideoError = (index: number) => {
  console.error(`视频播放错误: ${index}`);
  reloadVideo(index);
};

// 安全播放函数
const safePlay = (video: HTMLVideoElement): Promise<void> => {
  return new Promise((resolve) => {
    const playAttempt = () => {
      video
        .play()
        .then(resolve)
        .catch((error) => {
          console.error("视频播放失败:", error);
          if (error.name === "AbortError" || error.name === "NotAllowedError") {
            setTimeout(playAttempt, 500);
          } else {
            // 其他错误处理
            const index = parseInt(video.dataset.index || "0");
            reloadVideo(index);
          }
        });
    };

    playAttempt();
  });
};

// 心跳检测 - 监控卡顿的视频
const startHeartbeat = () => {
  const intervalId = setInterval(() => {
    currentVideos.value.forEach((video) => {
      if (video.readyState > 0 && video.paused) {
        console.warn("检测到卡顿视频，重新加载");
        const index = parseInt(video.dataset.index || "0");
        reloadVideo(index);
      }
    });
  }, 5000);

  return intervalId;
};

let heartbeatInterval: number | null = null;

onMounted(() => {
  initVideoCells();
  heartbeatInterval = startHeartbeat() as unknown as number;
});

// 清理
onUnmounted(() => {
  // 停止心跳检测
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval);
  }

  // 清理所有视频资源
  videoCells.value.forEach((cell) => {
    if (cell.current?.blobUrl) {
      URL.revokeObjectURL(cell.current.blobUrl);
    }
    if (cell.next?.blobUrl) {
      URL.revokeObjectURL(cell.next.blobUrl);
    }
  });

  // 清理缓存资源
  videoCache.value.forEach((item) => {
    if (item.blobUrl) {
      URL.revokeObjectURL(item.blobUrl);
    }
  });
});
</script>

<style scoped>
.video-wall-container {
  height: 100vh;
  width: calc(100vh * 9 / 16); /* 9:16 竖屏比例 */
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
  transition: opacity 0.3s ease-in-out;
}

.video-element.next {
  opacity: 0;
  z-index: 1;
}

.video-element.active {
  opacity: 1;
  z-index: 2;
}

.transition-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: white;
  z-index: 10;
  opacity: 0;
  animation: fadeTransition 0.3s ease-in-out;
}

@keyframes fadeTransition {
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
</style>
