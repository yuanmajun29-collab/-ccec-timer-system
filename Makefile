# CCEC 工位倒计时系统 — 常用命令（需 Docker + docker compose）
.PHONY: help check setup up down restart logs ps health prod prod-down wait urls

help:
	@echo "make check   - 检查 Docker / 端口"
	@echo "make setup   - 首次全量搭建（compose up + 等待健康）"
	@echo "make up      - 等同 bootstrap：检查、启动、等待健康、打印地址"
	@echo "make down    - 停止并移除容器"
	@echo "make logs    - 跟踪全部日志（可加 SVC=timer-backend）"
	@echo "make ps      - 容器状态"
	@echo "make health  - 探测本机 BACKEND_PORT 健康（读 .env）"
	@echo "make prod    - 生产编排 + 监控栈"
	@echo "make urls    - 仅打印访问地址"

check:
	@./deploy/scripts/prerequisites.sh

setup:
	@chmod +x deploy/scripts/*.sh deploy/scripts/lib/*.sh 2>/dev/null || true
	@./deploy/scripts/setup-local.sh

up: check
	@chmod +x deploy/scripts/*.sh deploy/scripts/lib/*.sh 2>/dev/null || true
	@./deploy/scripts/bootstrap.sh

down:
	@./deploy/scripts/stop.sh

restart: down up

logs:
	@./deploy/scripts/logs.sh $(SVC)

ps:
	@docker compose ps

health:
	@./deploy/scripts/wait-for-backend.sh 5 1 || true

urls:
	@./deploy/scripts/print-endpoints.sh

prod:
	@chmod +x deploy/scripts/*.sh deploy/scripts/lib/*.sh 2>/dev/null || true
	@./deploy/scripts/setup-prod-stack.sh

prod-down:
	@docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env down
