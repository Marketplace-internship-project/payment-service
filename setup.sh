#!/bin/bash

# Цвета для красивого вывода
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Запоминаем текущую папку (payment-service)
CURRENT_DIR_NAME=${PWD##*/}
CURRENT_DIR_PATH=$(pwd)

echo -e "${BLUE}=== Marketplace Quick Start Setup ===${NC}"

# ---------------------------------------------------------
# 1. Проверка и создание .env
# ---------------------------------------------------------
if [ -f .env.example ] && [ ! -f .env ]; then
    echo -e "${GREEN}[Config] Creating .env file from .env.example...${NC}"
    cp .env.example .env
else
    echo -e "${BLUE}[Config] .env file check passed.${NC}"
fi

# ---------------------------------------------------------
# 2. Клонирование репозиториев
# ---------------------------------------------------------
# Переходим на уровень выше, чтобы положить другие сервисы РЯДОМ с payment-service
cd ..

clone_repo() {
    SERVICE_NAME=$1
    REPO_URL=$2

    if [ ! -d "$SERVICE_NAME" ]; then
        echo -e "${GREEN}[Cloning] $SERVICE_NAME...${NC}"
        git clone "$REPO_URL"
    else
        echo -e "${BLUE}[Skipping] $SERVICE_NAME directory already exists.${NC}"
    fi
}

echo -e "${GREEN}>>> Checking sibling repositories...${NC}"

# User Service
clone_repo "user-service" "https://github.com/Marketplace-internship-project/user-service.git"

# Authentication Service
clone_repo "authentication-service" "https://github.com/Marketplace-internship-project/authentication-service.git"

# Order Service
clone_repo "order-service" "https://github.com/Marketplace-internship-project/order-service.git"

# API Gateway (Этого не хватало в твоем скрипте)
clone_repo "api-gateway" "https://github.com/Marketplace-internship-project/api-gateway.git"

# Возвращаемся обратно в папку payment-service
cd "$CURRENT_DIR_PATH" || exit

# ---------------------------------------------------------
# 3. Подготовка скриптов
# ---------------------------------------------------------
# Делаем init-db.sh исполняемым (для Linux/Mac это важно, для Windows не повредит)
if [ -f init-db.sh ]; then
    chmod +x init-db.sh
fi

# ---------------------------------------------------------
# 4. Запуск Docker Compose
# ---------------------------------------------------------
echo -e "${GREEN}[Start] Starting all services with Docker Compose...${NC}"
echo -e "${BLUE}Make sure Docker Desktop is running!${NC}"

# Запускаем сборку и поднятие контейнеров
# -d можно добавить, если хочешь запустить в фоне (detached mode)
docker-compose up --build