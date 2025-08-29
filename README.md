# License Platform - Spring Boot 授权机制

一个基于 Spring Boot 实现的企业级 License 授权平台，支持私钥签发、RSA验签、机器绑定、功能控制、时间限制、时间回拨检测、防篡改等多维防护能力，适用于私有化部署、商业授权验证等场景。

## 功能特性

- RSA 非对称加密授权签发与验签
- License 文件结构支持多字段（功能、绑定设备、到期时间等）
- 启动时自动校验 License，有效性失败拒绝服务启动
- 支持硬件绑定（MAC / CPU / 主板序列号）
- 支持功能权限开关（如 exportExcel、高级报表）
- 支持精确到秒的授权有效期与时间回拨检测
- LicenseContext + 全局拦截器 防绕过机制
- 拆分 SDK 可复用于多个微服务
- 丰富的注释 + 清晰的目录结构，适合初学者学习和改造

## 项目结构

```
src/main/java/org/example/licenseplatform
├── client         # 客户端 License 校验模块
├── common         # 通用返回结构、错误码
├── config         # 配置类（License 配置 / MVC 拦截器注册）
├── context        # License 上下文，记录是否已授权
├── controller     # 接口层（签发 / 验证 / 获取机器指纹）
├── handler        # 全局异常处理器
├── interceptor    # HTTP 请求拦截器（校验授权状态）
├── model          # LicenseRequest、LicenseContent 等结构体
├── service        # License 签发 & 校验核心服务
├── util           # 加解密工具、系统信息工具、时间校验工具等
└── LicensePlatformApplication.java
```

## 快速上手

### 1. 克隆项目

```
git clone https://github.com/luokakale-k/license-demo.git
cd license-platform
```

### 2. 修改配置

编辑 `application.yml`：

```
license:
  publicKeyPath: /path/to/publicCerts.keystore
  licensePath: /path/to/license.lic
  timeRecordPath: /tmp/license.time.record
```

### 3. 启动应用

```
mvn spring-boot:run
```

或

```
java -jar target/license-platform.jar
```

## License 授权机制流程

```
[客户端机器]            [授权平台]                 [客户端项目]
  └─ 采集硬件指纹  →     └─ 签发 license 文件  →     └─ 校验并运行
                           (私钥签名)                   (公钥验签、时间/功能/指纹校验)
```

## 主要接口说明

| 接口路径        | 方法 | 描述                    |
| --------------- | ---- | ----------------------- |
| /machine/info   | GET  | 获取当前机器指纹信息    |
| /license/issue  | POST | 生成授权文件（.lic）    |
| /license/verify | POST | 服务端验证 License 文件 |

## 安全机制设计

- 机器绑定：License 中包含 machineId，使用 MAC+CPU+主板信息生成指纹
- 时间限制：支持精确到秒的 expireDate
- 时间回拨检测：通过 timeRecord 文件记录最大启动时间戳
- 签名防篡改：使用私钥签发、公钥验签，防止修改内容
- 功能控制字段：License 支持功能模块控制（如 exportExcel: true）
- 反编译防护：支持 ProGuard / XJar / yGuard 混淆增强安全性
- 防绕过机制：LicenseBootChecker + LicenseVerifyInterceptor 双保险

## 构建打包（可选开启混淆）

### 1. 直接打包

```
mvn clean package -DskipTests
```

### 2. 可选：启用混淆（ProGuard 或 yGuard）

pom.xml 中配置插件：

```
<plugin>
  <groupId>com.yworks</groupId>
  <artifactId>yguard</artifactId>
  <version>4.0.0</version>
  ...
</plugin>
```

## TODO

- License 文件找回机制
- 管理后台页面
- License SDK 发布到 Maven 中央库
- 文档中增加使用示例（Demo 工程）
- 支持 License 文件加密存储
- 接入微信/邮箱通知授权到期

## License

MIT License

## 联系交流

欢迎提 Issue / PR