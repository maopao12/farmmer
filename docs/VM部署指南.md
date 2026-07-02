# 智慧农业系统 - 虚拟机部署指南

## 一、环境要求

| 组件 | 主机A（后端+数据库） | 主机B（可选-MQTT） |
|------|---------------------|---------------------|
| OS | Ubuntu 22.04 / CentOS 7+ | Ubuntu 22.04 |
| JDK | 21 | — |
| MySQL | 8.0 | — |
| MQTT Broker | — | EMQX / Mosquitto |

## 二、快速部署（本地开发）

```bash
# 1. 初始化数据库
mysql -u root -p123456 < smart-agriculture-server/src/main/resources/db/schema.sql
mysql -u root -p123456 < smart-agriculture-server/src/main/resources/db/data.sql
mysql -u root -p123456 < smart-agriculture-server/src/main/resources/db/test-data.sql

# 2. 启动后端
cd smart-agriculture-server
../mvnw spring-boot:run

# 3. 启动前端
cd smart-agriculture-web
npm install && npm run dev

# 访问 http://localhost:5173
```

## 三、虚拟机部署

### 场景A：单VM部署（后端 + MySQL + 前端在同一VM）

```bash
# 1. 启动后端（监听 0.0.0.0:8080，默认配置已支持）
java -jar smart-agriculture-server/target/*.jar

# 2. 构建前端
cd smart-agriculture-web
# 编辑 .env.production，设置后端地址
echo "VITE_API_BASE_URL=http://192.168.56.101:8080" > .env.production
npm run build

# 3. 将 dist/ 部署到 Nginx
cp -r dist/* /var/www/html/
```

### 场景B：多VM分离部署

**VM1 - 数据库 (192.168.56.100)**:
```bash
# MySQL 已安装运行
mysql -u root -p < schema.sql
mysql -u root -p < data.sql
```

**VM2 - 后端 (192.168.56.101)**:
```bash
export DB_HOST=192.168.56.100
export DB_PORT=3306
export SERVER_ADDRESS=0.0.0.0
java -jar smart-agriculture-server.jar
```

**VM3 - 前端 (192.168.56.102)**:
```bash
# .env.production
VITE_API_BASE_URL=http://192.168.56.101:8080
npm run build
# 部署到 Nginx
```

**VM4 - MQTT Broker (192.168.56.103)**:
```bash
# EMQX 或 Mosquitto，端口 1883
docker run -d --name emqx -p 1883:1883 -p 8083:8083 emqx/emqx:latest
```

### 场景C：基地硬件对接

```bash
# 启动后端（连接MQTT）
export SPRING_PROFILES_ACTIVE=prod
export MQTT_ENABLED=true
export MQTT_BROKER=tcp://192.168.56.103:1883
export DB_HOST=192.168.56.100
java -jar smart-agriculture-server.jar

# MqttDataCollector 自动订阅传感器数据
# DataCollector接口切换: application.yml → data.collector=mqtt
```

## 四、关键环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SERVER_ADDRESS` | `0.0.0.0` | 后端绑定地址 |
| `SERVER_PORT` | `8080` | 后端端口 |
| `DB_HOST` | `localhost` | MySQL主机地址 |
| `DB_PORT` | `3306` | MySQL端口 |
| `DB_USER` | `root` | 数据库用户 |
| `DB_PASS` | `123456` | 数据库密码 |
| `MQTT_ENABLED` | `false` | 是否启用MQTT |
| `MQTT_BROKER` | `tcp://localhost:1883` | MQTT Broker地址 |
| `SPRING_PROFILES_ACTIVE` | `mock` | 环境: mock/prod |
| `SQL_INIT_MODE` | `always` | 数据库初始化: always/never |

## 五、验证命令

```bash
# 登录
curl -X POST http://192.168.56.101:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 查询地块
curl http://192.168.56.101:8080/api/v1/plot/all \
  -H "Authorization: Bearer {token}"

# WebSocket
wscat -c ws://192.168.56.101:8080/ws
```

## 六、常见问题

| 问题 | 解决 |
|------|------|
| 前端无法连接后端 | 检查 CORS（已配置 *）、检查防火墙 8080 端口 |
| WebSocket连接失败 | 检查 Nginx 是否转发 WebSocket Upgrade 头 |
| MQTT无数据 | 检查 MQTT_ENABLED=true、MQTT_BROKER地址正确 |
| 数据库连接拒绝 | MySQL需允许远程连接: `GRANT ALL ON *.* TO 'root'@'%'` |
