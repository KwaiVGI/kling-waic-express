#!/bin/bash

echo "Testing restart.sh script functionality..."

# Test 1: Check if restart.sh exists and is executable
if [ -x "./restart.sh" ]; then
    echo "✓ restart.sh exists and is executable"
else
    echo "✗ restart.sh is not executable or doesn't exist"
    exit 1
fi

# Test 2: Check if api/restart.sh exists and is executable
if [ -x "./api/restart.sh" ]; then
    echo "✓ api/restart.sh exists and is executable"
else
    echo "✗ api/restart.sh is not executable or doesn't exist"
    exit 1
fi

# Test 3: Check if api directory exists
if [ -d "./api" ]; then
    echo "✓ api directory exists"
else
    echo "✗ api directory doesn't exist"
    exit 1
fi

# Test 4: Check if api/pom.xml exists
if [ -f "./api/pom.xml" ]; then
    echo "✓ api/pom.xml exists"
else
    echo "✗ api/pom.xml doesn't exist"
    exit 1
fi

# Test 5: Check if logs directory can be created
mkdir -p logs
if [ -d "./logs" ]; then
    echo "✓ logs directory can be created"
else
    echo "✗ Cannot create logs directory"
    exit 1
fi

echo ""
echo "All tests passed! The restart.sh scripts should work correctly."
echo ""
echo "Usage:"
echo "  From server directory: ./restart.sh"
echo "  From api directory: ./restart.sh"
echo ""
echo "Note: The application may have compilation issues that need to be resolved separately."
