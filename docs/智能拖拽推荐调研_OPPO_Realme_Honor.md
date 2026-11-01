# 智能拖拽推荐调研：OPPO ContentPortal / Realme RomCommonService / Honor Magic Portal + 智慧决策

> 调研日期：2026-11-01  
> 目标：分析三套系统级“拖拽内容 → 推荐 app/dock target”的实现方式，判断哪些能本地复现，并为 ohmyAES 选择可落地的方案。

---

## 0. 结论摘要

| 方案 | 推荐规则来源 | 文本/图片理解 | 是否依赖云端 | 可移植性 | 智能程度 |
|---|---|---|---|---|---|
| OPPO ContentPortal 17.4.2 | 云端 `scene_rules_url` + 少量本地预置 | OIntent / AI Unit 本地实体识别 | **是**（导航等规则在云端） | 低 | 中高（有云端规则时） |
| Realme RomCommonService 15.0.7 | **APK 内本地 XML** | 本地 OIntentApi + NlpNerUtil | **基本否**（仅 OTA XML 更新） | **高** | 中 |
| Honor Magic Portal 17.26.9.301 + 智慧决策 11.0.0.739 | **本地规则/模型 + 外部 `com.hihonor.brain`** | 本地 NLU + LightGBM 排序 + 可选云端 NLU/LLM/VLM | **混合**（本地 Brain + 云控增强） | 低 | **最高** |

最终建议：

1. **ohmyAES 采用 Realme 路线**：本地 XML 规则 + 本地实体分类 + UsageStats + 深链。
2. OPPO 的云端规则接口不可用，不能作为产品依赖。
3. Honor 是“本地 Brain 引擎 + 可选云端增强”，比 Realme 更智能，但 serviceId/intent/native model 体系无法在 Xiaomi 上直接复现。
4. Honor 的本地 `rule_config.json` / `algo_config.json` 可以作为设计参考，但不能直接当 Android `ACTION_SEND` 规则表。

---

## 1. OPPO ContentPortal 17.4.2

APK：

```text
oppo/内容传送门_17.4.2.apk
package: com.oplus.contentportal
versionName: 17.4.2
```

### 1.1 核心类

```text
com.oplus.contentportal.utils.k
    CheckDragUtils.kt
    - h(): 生成 recommendedType 集合
    - d(): 读取 ClipData 文件类型
    - g(): OIntent 实体 → 中文语义标签

com.oplus.contentportal.permanent.models.RecommendServiceModel
    - A(): 加载 fixed + selection + recommend 服务
    - z(): 过滤单个服务
    - s(): 分桶排序
    - e()/p()/t(): 推荐/选择服务

com.oplus.contentportal.service.ContentPortalService
    - checkDataValid runnable 把 numberType/fileTypes 传给 RecommendServiceModel.A()

com.oplus.contentportal.permanent.repository.datafusion.OnLineDataFusionManager
    - 云端规则下载、缓存、DB 写入
```

### 1.2 识别层

`CheckDragUtils.h()` 生成 `recommendedType`：

```text
ADDRESS        -> "地址"
WEB_URL        -> "网页链接"
PHONE          -> "电话号码"
EMAIL          -> "邮箱"
SCHEDULE       -> "日程"
FIGHT_NUM      -> "航班号"
TRAIN_NUM      -> "火车班次"
EXPRESS_NUM    -> "快递单号"
TAOBAOTOKEN    -> "淘口令"
QUARKTOKEN     -> "网盘链接"
ALIPAYTOKEN    -> "吱口令"
TIKTOKTOKEN    -> "抖口令"
XHSTOKEN       -> "小红书口令"
CURRENCY_RATE  -> "换算"
```

另外还有：

```text
i(): 扫一扫二维码 → 微信二维码 / 支付宝二维码 / 其他二维码
b(): AI 商品图片
```

地址识别由 `AddressIntentDetector` 完成，内部调用 OPPO AI Unit 的 `q4/a.s(text)`，返回地址 span + specType。

### 1.3 过滤层

`RecommendServiceModel.z()` 的条件：

```text
service.isSupportService
&& (service.serviceIdentifyValue.isEmpty()
    || service.serviceIdentifyValue in recommendedType)
&& service.supportType 支持当前 fileTypes
&& (service.number == numberType || service.number == "不涉及")
&& 拖拽来源 package 没有被 source / forbidSource 限制
```

字段含义：

| 字段 | 含义 |
|---|---|
| `serviceIdentifyValue` | 语义类型，例如 `地址`、`网页链接`、`电话号码` |
| `supportType` | 支持的文件类型，逗号分隔 |
| `number` | `单个` / `多个` / `不涉及` |
| `sort` | `P0` / `P1` / `P2` |
| `source` / `forbidSource` | 拖拽来源包名限制 |
| `serviceType` | 固定推荐 / 通用推荐 |
| `action` / `deeplink` | 点击行为 |

注意：`numberType` 不是“地址/电话”语义，而是：

```text
ClipData.getItemCount() == 1 ? "单个" : "多个"
```

语义类型来自 `recommendedType`。

### 1.4 排序层

`RecommendServiceModel.s()` 的分桶：

```text
1. filteredUnmatched
   serviceIdentifyValue 非空且命中 recommendedType
   按 sort P0/P1/P2

2. topByLaunchCount
   serviceIdentifyValue 为空的通用服务，取启动次数前 3

3. restByUpdateTime
   再按最近使用时间取 2

4. restP0BySort
   剩下 P0

5. restP1BySort
   剩下 P1

6. restBySort
   其余服务

最后去重，最多 15 个。
```

因此地址场景：

```text
recommendedType = {"地址"}
高德/百度/腾讯地图规则 serviceIdentifyValue = "地址"
→ 进入 filteredUnmatched
→ 排在微信/QQ 等通用服务前面
```

### 1.5 规则来源：云端，不是 APK 本地

APK 自带的预置数据只有：

```text
PreSetDataUtils
- 创建笔记
- 创建相册
- 分享
- 中转站
- 小布助手
```

真正的导航/浏览器规则来自：

```text
OnLineDataFusionManager
  baseUrl = https://sidebar-apps-cn.allawnos.com
  GET /api/os-sider/ytzd/getYTZDConfig
```

请求参数：

```text
model
colorOSVersion
androidVersion
grayNumber
apkVersion
brand
language
appId=sidebar-client
timestamp
sign
hash（可选）
```

签名：

```text
sign = SHA-256(
    appId
    + timestamp
    + (hash ? "hash=" + hash + "&" : "")
    + body
    + secret
)
secret = 945d0033
```

实际测试：

```text
CN 域名 sidebar-apps-cn.allawnos.com -> 公网 DNS NXDOMAIN
sidebar-core-server.wanyol.com      -> 10.52.37.150，内网不可达
EU/IN/US 域名                       -> HTTP 500 Internal Server Error
POST                                 -> 405 Method Not Allowed（确认是 GET）
```

结论：

- 公网无法取得 OPPO 云控规则。
- 只复制 `RecommendServiceModel` 不会得到导航推荐，因为规则表是空的。
- 除非从真实 OPPO 设备导出 `recommended_rule_info`，否则不要依赖这条路线。

---

## 2. Realme RomCommonService 15.0.7

APK：

```text
oppo/RomCommonService.apk
package: com.oplus.rom
versionName: 15.0.7
android:persistent=true
```

### 2.1 核心类

```text
com.oplus.rom.utils.y0
    SpaceRouletteAppListUtils
    - 解析 sys_space_roulette_custom_config.xml
    - 支持本地 asset 和 ROM OTA provider 更新

Lbb/c
    AppShareHelper
    - 解析 ACTION_SEND
    - PackageManager 枚举候选 app
    - 本地打分、排序

Lha/y
    FloatingManager
    - SpaceRoulette 浮窗 / 圆形菜单

Ll9/a
    OIntentApi sdk 1.1.5-beta
    - 本地文本实体识别

Lbb/e
    NlpNerUtil
    - 调用 OIntentApi
```

### 2.2 本地 XML

APK 内直接包含：

```text
assets/sys_space_roulette_custom_config.xml
大小：9,793 bytes
版本：2024052006
```

节点：

```xml
<app_sort>
    <type_text name="..."/>
    <type_pic name="..."/>
    <type_other name="..."/>
</app_sort>

<app_info>
    <app_type key="包名" val="类型ID"/>
</app_info>

<app_sort>
    <share_weight key="setting_score" val="1.0"/>
    <share_weight key="share" val="2.0"/>
</app_sort>

<app_action>
    <scheme_support_config key="包名" val="deeplink"/>
</app_action>

<app_pic_type>
    <ai_pic_type key="..." val="..."/>
</app_pic_type>
```

`type_text` 是文本类 app 的**本地优先级列表**，包含：

```text
com.tencent.mm
com.tencent.mobileqq
com.baidu.searchbox
com.tencent.mtt
com.alibaba.android.rimet
com.quark.browser
com.tencent.androidqqmail
com.netease.mail
com.coloros.note
com.android.mms
com.android.email
com.sina.weibo
com.heytap.browser
com.UCMobile
com.zhihu.android
com.ss.android.article.news
com.xingin.xhs
com.ss.android.ugc.aweme
com.smile.gifmaker
com.kuaishou.nebula
tv.danmaku.bili
com.xunmeng.pinduoduo
com.taobao.taobao
com.jingdong.app.mall
...
```

`app_info` 中的 app 类型：

```xml
<app_type key="com.google.android.dialer" val="21"/>
<app_type key="com.google.android.apps.maps" val="11"/>
<app_type key="com.google.android.calendar" val="26"/>
<app_type key="com.heytap.browser" val="14"/>
<app_type key="com.android.chrome" val="14"/>
<app_type key="com.google.android.gm" val="8"/>
<app_type key="com.eg.android.AlipayGphone" val="30"/>
```

`app_action` 中的深链：

```xml
<scheme_support_config key="com.sdu.didi.psnger"
    val="OneTravel://dache_anycar/sendorder?source=gs-openapi&amp;q="/>
<scheme_support_config key="com.sankuai.meituan"
    val="imeituan://www.meituan.com/search?q="/>
<scheme_support_config key="com.tencent.map"
    val="geo:0,0?q="/>
<scheme_support_config key="com.google.android.apps.maps"
    val="geo:0,0?q="/>
<scheme_support_config key="com.xingin.xhs"
    val="xhsdiscover://search/result?keyword="/>
```

### 2.3 运行时链路

`AppShareHelper.v()`：

```text
ClipData / ACTION_SEND
    -> PackageManager.queryIntentActivities()
    -> NlpNerUtil / OIntentApi 识别文本类型
    -> 读取 type_text / type_pic / app_info / share_weight / app_action
    -> UsageStatsManager 最近 5 天使用情况
    -> AppShareHelper.C()/h() 计算分数
    -> AppShareHelper.E() 排序
```

### 2.4 地址 → 导航

可还原的逻辑：

```text
拖拽文本包含地址
    -> OIntentApi 识别为地址
    -> AppShareHelper.j(Intent) 返回 Lab/b(11, ...)
    -> AppShareHelper.A(pkg, Lab/b)
       如果 IOplusDeepThinkerManager.getAppType(pkg) == 11，加 1.0 分
    -> app_action 中的地图包加入候选：
       com.tencent.map              -> geo:0,0?q=
       com.google.android.apps.maps -> geo:0,0?q=
    -> 排序后地图 app 提前
```

注意：

- Realme 自带 XML 只显式包含腾讯地图和 Google Maps，**没有高德/百度**。
- 移植到国内环境应补：

```text
com.autonavi.minimap   高德
com.baidu.BaiduMap     百度
com.tencent.map        腾讯
```

### 2.5 本地/云端判断

**基本本地。**

```text
assets/sys_space_roulette_custom_config.xml  本地
OIntentApi / NlpNerUtil                      本地 SDK
PackageManager 候选 app                      本地
UsageStatsManager                           本地
IOplusDeepThinkerManager.getAppType          本地 app 分类
Lbb/d SharedPreferences                     本地权重
```

仅有：

```text
content://com.oplus.romupdate.provider.db/update_list
    -> ROM OTA 的 sys_space_roulette_custom_config 更新
```

这是系统 OTA 配置更新，不是实时推荐 API。

`OIntent` 的 token 正则规则可能从：

```text
https://iwisdom.apps.coloros.com/wisdom/pwd/rules
```

更新，但只影响 token 识别，不影响 app 列表/排序主体。

---

## 3. Honor Magic Portal 17.26.9.301

APK：

```text
oppo/荣耀任意门-17.26.9.301(172609301).apk
package: com.hihonor.hnmagicportal
versionName: 17.26.9.301
minSdk/targetSdk: 36
```

对应的推荐引擎 APK：

```text
oppo/智慧决策_11.0.0.739_e51be69b-bc1f-4d94-bbd7-343679fa39d5.apk
package: com.hihonor.brain
versionName: 11.0.0.739
```

注意：MagicPortal 本身只是客户端；`com.hihonor.brain` 才是“任意门/多推荐”的决策引擎。  
`com.hihonor.brain` 不是纯云服务，APK 内包含大量本地规则、权重和 LightGBM 模型。

### 3.1 核心类

```text
com.hihonor.brain.BrainClient
com.hihonor.hntransgate.serviceload.BrainConnectUtil
com.hihonor.hntransgate.serviceload.DockInfoProcessUtil
com.hihonor.hntransgate.serviceload.ServiceLogicUtil
com.hihonor.hntransgate.ui.view.dock.DockItemInfo

com.hihonor.brain.business.decision.multifusion.multimode.asm.MmSubAsm
com.hihonor.brain.business.decision.multifusion.multimode.trigger.MmDispatchTrig
com.hihonor.brain.business.decision.multifusion.multimode.module.identify.business.IdentifyBiz
com.hihonor.brain.business.decision.multifusion.multimode.module.decision.business.ServiceSortBiz
com.hihonor.brain.business.decision.multifusion.multimode.algorithm.train.MixedModelTrain
com.hihonor.brain.business.decision.multifusion.multimode.algorithm.rule.RuleManager
com.hihonor.brain.business.decision.multifusion.multimode.util.NluClientUtil
com.hihonor.brain.business.decision.multifusion.multimode.util.CloudUtil
```

### 3.2 不是自己算推荐，而是请求 `com.hihonor.brain`

`BrainConnectUtil.connectBrain()` 构造请求：

```text
serviceName   = MULTI_RECOMMEND_SERVICE
abilityName   = RECOMMEND_EXEC
business      = transferGate
content       = DockInfoProcessUtil.getContentInfo()
callingPkg    = com.hihonor.hnmagicportal
isParceled    = true
serviceFilter = ServiceFilter(serviceType=[1,2], installed=1, useAble=1, argumentMatched=1)
isUseLlm      = CommonUtils.isUseLlm()
isUseNluCloud = CommonUtils.isUseNluCloud()
isUseVlm      = CommonUtils.isUseVlm()
recommendId   = UUID
```

然后：

```java
BrainClient.executeTrigger(
    "com.hihonor.brain.decision.MULTI_RECOMMEND",
    "MmSubAsm",
    requestMap,
    callback,
    2200ms
);
```

`BrainClient` 绑定的是：

```text
com.hihonor.brain.BrainService
action: com.hihonor.brain.action.BIND_BRAIN_SERVICE
```

`BrainConnectUtil` 回调收到 `ParceledListSlice` 的 `DockItemInfo` 列表，再交给：

```text
DockInfoProcessUtil.createDockItemInfos(...)
```

如果 Brain 未连接、超时或返回空：

```text
addDefaultItem(ClipData)
mDefaultDockItems
```

即回退到本地默认服务列表。

### 3.3 云端能力开关

`CommonUtils` 直接读取 Honor 云控配置：

```java
isUseLlm():
  getCloudCfgBoolValue("/magicgate/", "is_use_llm", false)
  或 is_use_llm_chn / is_use_llm_ovs

isUseNluCloud():
  getCloudCfgBoolValue("/magicgate/", "is_use_nlu_cloud", false)
  海外版本直接 false

isUseVlm():
  getCloudCfgBoolValue("/magicgate/", "is_use_vlm", false)
  海外版本直接 false
```

因此：

- **MagicPortal 本身不是纯本地规则引擎。**
- 完整 Honor 栈是混合架构：`com.hihonor.brain` 内部有本地规则/模型，云端 NLU/LLM/VLM 由机型、区域和 Honor 云控决定。
- MagicPortal APK 只负责内容采集、请求、结果渲染和点击执行；核心推荐在 `com.hihonor.brain`。

### 3.4 本地部分

Honor 并不是完全空壳，本地还有：

1. `DockInfoProcessUtil` 解析 ClipData、生成内容类型、默认 dock item：
   - 收藏空间 `com.hihonor.collectcenter`
   - 笔记 `com.hihonor.notepad`
   - 打印 `com.hihonor.printassistant`
   - YOYO `com.hihonor.magicvoice`
   - 相册 `com.hihonor.photos`
   - 微信好友/相册等 service id

2. `ServiceLogicUtil` 本地执行深链：
   - `triggerContactService`
   - `triggerDialService`
   - `triggerScheduleService`
   - `triggerSearchService`
   - `triggerSearchTradeService`
   - `triggerSendMsgService`
   - `triggerAlipayCodeService`
   - `triggerPrintService`
   - `triggerParseImageService`
   - `triggerParallelSpace`

3. `DockInfoProcessUtil` 对图片/GIF/多 URI/文本做本地解析和兜底。

4. `assets/app_functions.xml` / `app_functions_v2.xml` 是 **AppFunctions（LLM 工具调用）schema**，不是拖拽 app 规则表。

所以 Honor 的架构是：

```text
MagicPortal APK（本地采集/渲染/点击/兜底）
        ↓ Binder
com.hihonor.brain（推荐决策，可能本地+云端）
        ↓
DockItemInfo 列表
```

### 3.5 智慧决策 APK 里的本地资产

`com.hihonor.brain` 不是一个空壳云客户端，它内置了大量本地资产：

```text
assets/rule_config.json
assets/algo_config.json
assets/signal_weight.json
assets/package_name_mapping.json
assets/appGroupRecoList.json
assets/multifusion_cpcode_service.json
assets/model_v2.mod
assets/model_ImplicitFeedbackRank_v2.mod
```

关键内容：

#### `rule_config.json`

`multiRecConfList` 中 `business=transferGate` 的配置包含：

```text
contentTags:
  url / location / time / video / expressNo / flightNo /
  email / music / carBrand / carSeries / book / commodity /
  delicacy / phoneNum / multiLocation / baiduNetDiskUrl / normalUrl

contentTagIntent:
  location -> ADDRESS_SEARCH / CHECK_ROUTES / HAIL_TAXI / TOURIST_SERVICES
  phoneNum -> MANAGE_CONTACTS / SEND_SMS_MESSAGES / MAKE_CALLS
  email    -> SEND
  time     -> CREATE (日程)
  commodity -> PURCHASE
  delicacy -> ORDER (外卖)
  expressNo -> DELIVERY_STATUS
  flightNo -> QUERY_DETAIL
  ...
```

还有：

```text
textRecogMethods:
  nlu / tokenVerify / teddyTokenVerify / douTokenVerify /
  goodsRecog / delicacyRecog / translationRecog / regex

cvLabelIntent:
  商品 / 二维码 / 条码 / 车辆 / 人物 / 动物 / 植物 ...

vlmTag:
  location / flightNo / delicacy / commodity / book / video

sceneIntent:
  0..14 个场景对应不同的 intent 优先级
```

这说明 Honor 的地址理解不是简单正则，而是：

```text
文本/图片 → 本地 NLU / 云端 NLU / CV / VLM / token
         → contentTags + scene
         → 映射到具体 intent
```

#### `algo_config.json`

本地规则权重：

```text
DIFrequencyRule        0.5
DIDefaultRule          2
DIBlackListRule        100
DIServiceFrequencyRule 2
DIRecentServiceRule    3
DISceneRule            2
DIElementRule          4
DIAIRule               3
```

还有 `aiScoreMap`，直接给具体 serviceId 配置 AI 分数，例如：

```text
地址搜索 / 路线查询 → 负分（在非地址场景降权）
购物识别           → 100 / 99 / 98
扫码               → 98
```

#### 本地排序模型

```text
model_v2.mod                     ~119KB
model_ImplicitFeedbackRank_v2.mod ~212KB
```

两者都是 **LightGBM 文本模型**（`tree` / `version=v3` / `num_class=1`）。  
`ServiceSortBiz` 通过 `DIMixedModelTrain`、`MmAppRecUtil`、`AppActivityPredictData` 做 app 活跃度/排序预测；`signal_weight.json` 则是位置/信号权重矩阵。

#### `package_name_mapping.json`

包含国内常用 app 映射，例如：

```text
com.autonavi.minimap -> 高德地图
com.MobileTicket     -> 铁路12306
com.tongcheng.android -> 同程旅行
ctrip.android.view   -> 携程旅行
com.Qunar            -> 去哪儿网
com.sankuai.meituan  -> 美团
com.eg.android.AlipayGphone -> 支付宝
com.tencent.mm       -> 微信
```

#### `multifusion_cpcode_service.json`

包含支付宝、淘宝、抖音、荣耀商城、百度网盘、拼多多、京东、荣耀亲选的 CP code 实例，用于内容/服务匹配。

### 3.6 本地/云端判断

Honor 是**混合架构**，不是纯云，也不是像 Realme 那种纯本地 XML：

```text
MagicPortal APK
    本地：ClipData 解析、默认 dock、深链执行、fallback
        ↓ Binder
com.hihonor.brain APK
    本地：rule_config / algo_config / signal_weight /
          LightGBM models / 本地 NLU / 本地 service DB
    云端（可选）：
          /aicloud/nlu/extractInformation
          /aicloud/decision/v1/service/cp/meta/query
          /aicloud/operation/v1/configuration/universal/query
          /aicloud/ai-agent-service/v1/nlu/text
          /aicloud/... VLM / LLM
```

因此：

- **MagicPortal 单独看**：不是本地规则，核心在 Brain。
- **完整 Honor 栈**：本地 Brain 引擎已经能完成规则识别、LightGBM 排序、本地 NLU、service 查询；云端用于增强 NLU/LLM/VLM 和动态配置。
- `isUseNluCloud` / `isUseLlm` / `isUseVlm` 只是云端增强开关，不代表没有本地能力。
- 断网/Brain 超时后，MagicPortal 仍能回退到默认 dock items；但云端增强能力会消失。
- **可移植性仍然低**：Brain 使用 Honor 的 serviceId / intent / AppClip / native model 体系，不能直接搬进 Xiaomi/HyperOS。

---

## 4. 三者智能程度对比

### 4.1 智能程度排序

```text
Honor（本地规则 + LightGBM + 本地 NLU + 可选云端 NLU/LLM/VLM）
    > OPPO ContentPortal（云端规则 + OIntent）
    > Realme RomCommonService（本地 XML + OIntentApi + UsageStats）
```

### 4.2 本地/可移植性排序

```text
Realme（APK 内 XML，可完整读取）
    > OPPO（本地预置很少，核心规则云端）
    > Honor（本地 Brain 资产很多，但 serviceId/intent/native model 体系不可移植）
```

### 4.3 各自优劣

#### OPPO

优点：

- 规则模型清晰：`serviceIdentifyValue` / `supportType` / `number` / `sort`。
- 有服务级 deeplink、source 限制、启动次数、最近使用时间。
- 地址→导航的规则表达最直接。

缺点：

- 导航规则依赖云端，公网不可用。
- 没有云端缓存时只剩笔记/相册/分享等预置。

#### Realme

优点：

- 本地 XML 可直接读取、修改、移植。
- 本地 OIntentApi 做实体识别。
- PackageManager + UsageStats + app type，离线可用。
- 深链配置清楚，适合直接改造成 ohmyAES 的 TargetRule。

缺点：

- `type_text`/`type_pic` 是静态列表，语义能力有限。
- 默认配置以 realme/OPPO 海外预装 app 为主，需要补国内 app。
- 没有 LLM/VLM 那种复杂语义理解。

#### Honor

优点：

- 请求内容包含文本、文件、来源包、文件类型、ServiceFilter。
- **本地就有完整规则和排序模型**：`rule_config.json`、`algo_config.json`、`signal_weight.json`、两个 LightGBM 模型。
- `contentTags` → `contentTagIntent` 的映射非常完整：location/phone/email/time/commodity/delicacy/express/flight/video 等。
- 本地 NLU + 可选云端 NLU/LLM/VLM，能理解更复杂的内容和意图。
- 支持 AppClip、小程序、原生 deeplink、服务曝光/点击反馈。
- 有完整 fallback，Brain 超时也能显示默认服务。

缺点：

- 结构复杂，规则依赖 Honor serviceId / intent / AppClip 生态。
- 依赖 `com.hihonor.brain`，Xiaomi/HyperOS 没有对应服务。
- 云端 NLU/LLM/VLM 存在网络、隐私、延迟问题。
- 本地模型和 native lib 无法直接搬到 ohmyAES 的 SystemUI 模块。
- 可以借鉴规则结构，但不能直接复用它的 serviceId 和 intent。

---

## 5. 对 ohmyAES 的建议

### 5.1 采用 Realme 路线

建议新增：

```text
bridge/src/main/assets/target_rules.xml
```

结构参考：

```xml
<target_rules>
    <type_text name="com.tencent.mm"/>
    <type_text name="com.tencent.mobileqq"/>
    <type_pic name="com.miui.gallery"/>
    <type_pic name="com.miui.notes"/>

    <app_info>
        <app_type key="com.autonavi.minimap" val="11"/>
        <app_type key="com.baidu.BaiduMap" val="11"/>
        <app_type key="com.tencent.map" val="11"/>
        <app_type key="com.google.android.apps.maps" val="11"/>
    </app_info>

    <app_action>
        <scheme_support_config key="com.autonavi.minimap"
            val="androidamap://keywordNavi?sourceApplication=hyperportal&amp;keyword="/>
        <scheme_support_config key="com.baidu.BaiduMap"
            val="baidumap://map/geocoder?address="/>
        <scheme_support_config key="com.tencent.map"
            val="geo:0,0?q="/>
    </app_action>
</target_rules>
```

### 5.2 本地分类器

第一版只做：

```text
地址
URL
电话号码
邮箱
日程
图片/文本/文件
```

地址判定建议：

```text
包含 省/市/区/县/路/街/号/大厦/广场/小区/园区/楼/室
或 geo: / 经纬度
长度 4~80
排除纯数字、纯英文单词
```

后续再补：

```text
航班号 / 火车班次 / 快递单号 / 淘口令 / 网盘链接
```

### 5.3 排序

复刻 Realme/OPPO 的桶模型：

```text
1. 语义命中的 app（地址 → app type 11）
2. type_text / type_pic 中的本地优先级
3. UsageStats 最近使用
4. 其他可分享 app
5. 最多 15 个
```

### 5.4 点击行为

```text
地址   -> 高德/百度/腾讯地图深链
URL    -> 浏览器
电话   -> ACTION_DIAL
邮箱   -> mailto:
文本   -> ACTION_SEND
图片   -> ACTION_SEND + content URI
```

### 5.5 不建议

- 不要请求 OPPO `getYTZDConfig`：公网 500/域名不可达。
- 不要依赖 Honor `com.hihonor.brain`：Xiaomi 没有该服务。
- 不要把 Honor 的 `app_functions.xml` 当规则表：它是 AppFunctions/LLM schema。
- 不要直接搬 Honor 的 LightGBM `.mod` 和 native lib：模型特征依赖 Honor 的 serviceId/intent/AppClip 数据，无法在 ohmyAES 中直接推理。
- 可以借鉴 Honor 的 `contentTags -> contentTagIntent` 结构和 `weightMap`，但要重写成 ohmyAES 的本地规则。

---

## 6. 最终结论

```text
OPPO：算法完整，但导航规则在云端，公网拿不到。
Realme：本地 XML + 本地 NER + UsageStats，最适合移植。
Honor：本地规则 + LightGBM + 本地 NLU + 可选云端 NLU/LLM/VLM，最智能，但 serviceId/intent/native model 体系不可直接复现。
```

ohmyAES 应该走 **Realme 式本地规则引擎**，并补充国内导航/浏览器/邮箱/日历规则。  
Honor 的 `rule_config.json` / `algo_config.json` 适合作为“contentTags → intent → 权重”的设计参考，但 Honor 的 serviceId / AppClip / LightGBM 模型不能直接作为 ohmyAES 的依赖。

---

## 7. 拖到 QQ 为什么弹二次确认，Content Portal 怎么避免

### 7.1 根因：generic ACTION_SEND 只到“包”，没有到“页面”

如果代码只是：

```java
Intent intent = new Intent(Intent.ACTION_SEND);
intent.setType("text/plain");
intent.putExtra(Intent.EXTRA_TEXT, text);
intent.setPackage("com.tencent.mobileqq");
context.startActivity(intent);
```

QQ 的 manifest 里可能有多个 `ACTION_SEND` Activity，系统会解析到 QQ 的分享入口页。  
QQ 的分享入口页再让用户选择：

```text
发送给好友
QQ空间
收藏
...
```

这就是第二次确认。

`setPackage()` 只能限制“发给哪个 app”，不能指定“进哪个 Activity”。

### 7.2 Content Portal 的做法：ServiceInfo 带 className / deeplink

OPPO 的推荐规则不是“只配包名”，而是每个服务项都有：

```text
appPackage
action
deeplink
componenType
extraMap
  └─ class
serviceId
label
supportType
serviceIdentifyValue
```

`RecommendedRuleInfo` 转成 `ServiceInfo` 时：

```java
String className = extraMap.get("class");
serviceInfo.setClassName(className);
```

点击时 `JumpUtils.a()`：

```java
Intent intent = new Intent(action);
intent.setPackage(packageName);

if (className != null && className.length() > 0) {
    intent.setComponent(new ComponentName(packageName, className));
}

intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
```

有 `className` 时，是**显式 Component 启动**：

```text
直接打开 QQ 的某个具体 Activity
不经过 QQ 的分享入口页
不经过系统 Resolver
```

`deepLink` 路径则是：

```java
Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(deepLink));
intent.setPackage(packageName);
```

直接打开 QQ 的某个 scheme 页面。

### 7.3 QQ 的多个入口会被拆成多个 service

Content Portal 的 dock 里不只有一个“QQ”，而可以是：

```text
QQ-发送给好友
QQ-QQ空间
```

每个 service 有自己的：

```text
label
serviceId
action
extraMap.class
supportType
serviceIdentifyValue
```

用户在 Content Portal 的 dock 上先选“发送给好友”还是“QQ空间”，点击后直接进入对应页面。  
这样第二次确认被提前到了 Content Portal 的 target 选择阶段，QQ 内部不再弹。

QQ 的具体 `className` 在 APK 里没有硬编码，属于云端 `recommended_rule_info`，所以这里不猜具体类名；机制是 `extraMap.class`。

### 7.4 注意：不是所有情况都能避免

Content Portal 自己也有 fallback：

```text
JumpUtils.i() -> shareToSharePanel()
    -> Intent.createChooser(...)
```

当服务没有 `className`、没有 `deepLink`，或者显式 Activity 启动失败时，仍然会走通用分享面板。  
所以：

- 有明确目标页 → 显式 Component / deeplink → 无二次确认
- 只有包名 → 通用 ACTION_SEND → 目标 app 自己弹二次确认
- 目标 Activity 未导出 / 类名随版本变化 → 需要 deeplink 或 fallback

### 7.5 对 ohmyAES 的改造建议

TargetRule 增加：

```text
packageName
action
className     // 可选，优先于通用 ACTION_SEND
deepLink      // 可选
extraMap      // 文本 key、stream key、其他 extras
```

点击逻辑：

```java
if (deepLink != null) {
    intent = new Intent(ACTION_VIEW, Uri.parse(deepLink));
    intent.setPackage(packageName);
} else {
    intent = new Intent(action);
    intent.setPackage(packageName);
    if (className != null) {
        intent.setComponent(new ComponentName(packageName, className));
    }
}
```

QQ 先做两个独立 target：

```text
QQ-发送给好友
QQ-QQ空间
```

分别配置 QQ 对应 Activity 或 scheme；如果找不到稳定 Activity，就用 QQ 的 scheme，而不是退回 generic ACTION_SEND。

### 7.6 Honor 的做法：Brain 直接返回 NativeAppActivity

Honor MagicPortal 的 `DockItemInfo` 里带的是 `AppClipInfo`，不是单纯的包名。`AppClipInfo.deepLinkInfo` 可以包含：

```text
NativeAppActivity:
  pkgName
  activityName
  appName
  actionName
  parameterList

NativeAppLink:
  pkgName
  url

QuickAppLink / MiniProgramLink / WebLink
```

`ServiceLogicUtil.triggerService()` 的主路径：

```java
AppClipInfo appClipInfo = dockItemInfo.getAppClipInfo();
NativeAppActivity activity = appClipInfo.deepLinkInfo.nativeAppActivity;

if (activity != null && checkNativeAppActivityInfo(activity)) {
    if (activity.actionName != null) {
        return triggerCommonService(activity);
    }

    Intent intent = new Intent();
    intent.setComponent(
        new ComponentName(activity.pkgName, activity.activityName)
    );
    intent.setAction(shareType);
    setContentType(intent, contentType);
    // 加 content / content_type / share_type 等 extras
    return intent;
}
```

也就是说 Honor 和 OPPO 的核心一样：

```text
packageName 只说明“哪个 app”
activityName 才说明“哪个页面”
```

Brain 返回的 `NativeAppActivity` 已经带了 `activityName`，MagicPortal 直接显式启动它。  
如果这个入口在 Brain 的服务数据里是“QQ-发送给好友”的 Activity，就不会经过 QQ 的分享入口页，也就没有第二次选择。

Honor 还有 `NativeAppLink` / `QuickAppLink` / `MiniProgramLink` / `WebLink` 分支，没有原生 Activity 时可以用 scheme 打开具体页面。

结论：

```text
QQ 弹二次确认
= 只给了 package，QQ 自己找分享入口

Content Portal / Honor 不弹
= 规则里带了具体 Activity 或 deepLink
```

这不是靠 Android 的“默认应用”设置，也不是靠 `setPackage`，而是靠 **显式 Component**。
