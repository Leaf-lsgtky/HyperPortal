.class public final Lcom/miui/contentcatcher/service/config/a;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation build Landroid/annotation/TargetApi;
    value = 0xb
.end annotation

.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/miui/contentcatcher/service/config/a$c;,
        Lcom/miui/contentcatcher/service/config/a$b;,
        Lcom/miui/contentcatcher/service/config/a$a;
    }
.end annotation


# static fields
.field public static volatile g:Lcom/miui/contentcatcher/service/config/a;


# instance fields
.field public a:Landroid/content/Context;

.field public b:Landroid/content/SharedPreferences;

.field public c:Ljava/util/ArrayList;

.field public d:Ljava/lang/String;

.field public e:Ljava/lang/String;

.field public f:Lcom/miui/contentcatcher/service/config/ViewConfig;


# direct methods
.method public static a(Lmiui/contentcatcher/sdk/ClientCatcherResult;)Z
    .locals 8

    const-string v0, "ConfigHelper"

    const-string v1, "viewContent"

    const-string v2, "recomment"

    invoke-virtual {p0, v2}, Lmiui/contentcatcher/sdk/ClientCatcherResult;->getParams(Ljava/lang/String;)Ljava/util/Map;

    move-result-object p0

    if-nez p0, :cond_0

    const/4 p0, 0x1

    return p0

    :cond_0
    const-string v2, "pick_mode"

    invoke-interface {p0, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    const/4 v4, 0x0

    if-eqz v3, :cond_7

    :try_start_0
    invoke-interface {p0, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    instance-of v2, p0, Ljava/lang/CharSequence;

    if-eqz v2, :cond_1

    check-cast p0, Ljava/lang/CharSequence;

    invoke-interface {p0}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object p0

    goto :goto_2

    :catch_0
    move-exception p0

    goto :goto_3

    :cond_1
    instance-of v2, p0, Landroid/os/Bundle;

    const/4 v3, 0x0

    if-eqz v2, :cond_5

    check-cast p0, Landroid/os/Bundle;

    invoke-virtual {p0, v1}, Landroid/os/Bundle;->containsKey(Ljava/lang/String;)Z

    move-result v2

    if-eqz v2, :cond_2

    invoke-virtual {p0, v1}, Landroid/os/Bundle;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    goto :goto_2

    :cond_2
    const-string v1, "apkParamList"

    invoke-virtual {p0, v1}, Landroid/os/Bundle;->getStringArray(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v1

    if-eqz v1, :cond_6

    array-length v2, v1

    if-nez v2, :cond_3

    goto :goto_1

    :cond_3
    move v2, v4

    :goto_0
    array-length v5, v1

    if-ge v2, v5, :cond_5

    aget-object v5, v1, v2

    if-eqz v5, :cond_4

    const-string v6, "miui"

    invoke-virtual {v5, v6}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v5

    if-eqz v5, :cond_4

    aget-object v5, v1, v2

    invoke-virtual {v5}, Ljava/lang/String;->length()I

    move-result v5

    const/4 v6, 0x7

    if-le v5, v6, :cond_4

    aget-object v5, v1, v2

    const-string v7, "miuiCE_"

    invoke-virtual {v5, v7}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v5

    if-eqz v5, :cond_4

    aget-object v3, v1, v2

    invoke-virtual {v3, v6}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v3

    const-string v5, ""

    invoke-virtual {p0, v3, v5}, Landroid/os/Bundle;->getCharSequence(Ljava/lang/String;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;

    move-result-object v3

    invoke-interface {v3}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object v3

    :cond_4
    add-int/lit8 v2, v2, 0x1

    goto :goto_0

    :cond_5
    move-object p0, v3

    goto :goto_2

    :cond_6
    :goto_1
    return v4

    :goto_2
    invoke-static {p0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result p0

    if-eqz p0, :cond_7

    const-string p0, "getContent isEmpty"

    invoke-static {v0, p0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return v4

    :goto_3
    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, " compareText  e :"

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v0, p0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    :cond_7
    return v4
.end method

.method public static e(Landroid/content/Context;)Lcom/miui/contentcatcher/service/config/a;
    .locals 4

    sget-object v0, Lcom/miui/contentcatcher/service/config/a;->g:Lcom/miui/contentcatcher/service/config/a;

    if-nez v0, :cond_1

    const-class v0, Lcom/miui/contentcatcher/service/config/a;

    monitor-enter v0

    :try_start_0
    sget-object v1, Lcom/miui/contentcatcher/service/config/a;->g:Lcom/miui/contentcatcher/service/config/a;

    if-nez v1, :cond_0

    new-instance v1, Lcom/miui/contentcatcher/service/config/a;

    invoke-direct {v1}, Ljava/lang/Object;-><init>()V

    new-instance v2, Ljava/util/ArrayList;

    invoke-direct {v2}, Ljava/util/ArrayList;-><init>()V

    iput-object v2, v1, Lcom/miui/contentcatcher/service/config/a;->c:Ljava/util/ArrayList;

    const-string v2, ""

    iput-object v2, v1, Lcom/miui/contentcatcher/service/config/a;->d:Ljava/lang/String;

    iput-object v2, v1, Lcom/miui/contentcatcher/service/config/a;->e:Ljava/lang/String;

    iput-object p0, v1, Lcom/miui/contentcatcher/service/config/a;->a:Landroid/content/Context;

    const-string v2, "catcher_pref"

    const/4 v3, 0x0

    invoke-virtual {p0, v2, v3}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;

    move-result-object p0

    iput-object p0, v1, Lcom/miui/contentcatcher/service/config/a;->b:Landroid/content/SharedPreferences;

    sput-object v1, Lcom/miui/contentcatcher/service/config/a;->g:Lcom/miui/contentcatcher/service/config/a;

    goto :goto_0

    :catchall_0
    move-exception p0

    goto :goto_1

    :cond_0
    :goto_0
    monitor-exit v0

    goto :goto_2

    :goto_1
    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw p0

    :cond_1
    :goto_2
    sget-object p0, Lcom/miui/contentcatcher/service/config/a;->g:Lcom/miui/contentcatcher/service/config/a;

    return-object p0
.end method

.method public static j(Lmiui/contentcatcher/sdk/Content;)Ljava/util/HashMap;
    .locals 12

    const-string v0, "ConfigHelper"

    const-string v1, "#"

    if-eqz p0, :cond_a

    iget-object v2, p0, Lmiui/contentcatcher/sdk/Content;->mResultMap:Ljava/util/Map;

    if-nez v2, :cond_0

    goto/16 :goto_5

    :cond_0
    new-instance v3, Ljava/util/HashMap;

    invoke-direct {v3}, Ljava/util/HashMap;-><init>()V

    :try_start_0
    invoke-interface {v2}, Ljava/util/Map;->entrySet()Ljava/util/Set;

    move-result-object v4

    invoke-interface {v4}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v4

    :cond_1
    :goto_0
    invoke-interface {v4}, Ljava/util/Iterator;->hasNext()Z

    move-result v5

    if-eqz v5, :cond_6

    invoke-interface {v4}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Ljava/util/Map$Entry;

    invoke-interface {v5}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ljava/lang/String;

    invoke-virtual {v6, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v7

    if-nez v7, :cond_2

    goto :goto_0

    :cond_2
    invoke-virtual {v6, v1}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v6

    const-string v7, ""

    array-length v8, v6

    const/4 v9, 0x1

    const/4 v10, 0x0

    const/4 v11, 0x2

    if-ne v8, v11, :cond_3

    aget-object v8, v6, v10

    aget-object v6, v6, v9

    goto :goto_1

    :catch_0
    move-exception p0

    goto :goto_2

    :cond_3
    array-length v7, v6

    const/4 v8, 0x3

    if-ne v7, v8, :cond_1

    aget-object v8, v6, v10

    aget-object v7, v6, v9

    aget-object v6, v6, v11

    :goto_1
    invoke-interface {v5}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v5

    invoke-virtual {v3, v8}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z

    move-result v9

    if-nez v9, :cond_4

    new-instance v9, Lmiui/contentcatcher/sdk/ClientCatcherResult;

    invoke-direct {v9}, Lmiui/contentcatcher/sdk/ClientCatcherResult;-><init>()V

    invoke-virtual {p0}, Lmiui/contentcatcher/sdk/Content;->getInjectorToken()Lmiui/contentcatcher/sdk/Token;

    move-result-object v10

    invoke-virtual {v9, v10}, Lmiui/contentcatcher/sdk/ClientCatcherResult;->setInjectorToken(Lmiui/contentcatcher/sdk/Token;)V

    invoke-virtual {v3, v8, v9}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :cond_4
    const-string v9, "action"

    invoke-virtual {v9, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v9

    if-eqz v9, :cond_5

    const-string v6, "content"

    :cond_5
    invoke-virtual {v3, v8}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v8

    check-cast v8, Lmiui/contentcatcher/sdk/ClientCatcherResult;

    invoke-virtual {v8, v7, v6, v5}, Lmiui/contentcatcher/sdk/ClientCatcherResult;->addContent(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :goto_2
    new-instance v1, Ljava/lang/StringBuilder;

    const-string v4, " parseContent  e :"

    invoke-direct {v1, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v0, p0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    :cond_6
    invoke-virtual {v3}, Ljava/util/HashMap;->entrySet()Ljava/util/Set;

    move-result-object p0

    invoke-interface {p0}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :cond_7
    :goto_3
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_8

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/util/Map$Entry;

    if-eqz v1, :cond_7

    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v4

    if-eqz v4, :cond_7

    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lmiui/contentcatcher/sdk/ClientCatcherResult;

    invoke-virtual {v1}, Lmiui/contentcatcher/sdk/ClientCatcherResult;->getPropertyMap()Ljava/util/Map;

    move-result-object v1

    const-string v4, "isfullscreen"

    invoke-static {v2, v1, v4}, Lcom/miui/contentcatcher/service/config/a;->l(Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;)V

    const-string v4, "actionbarvisible"

    invoke-static {v2, v1, v4}, Lcom/miui/contentcatcher/service/config/a;->l(Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;)V

    const-string v4, "isedittext"

    invoke-static {v2, v1, v4}, Lcom/miui/contentcatcher/service/config/a;->l(Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;)V

    const-string v4, "coordinate"

    invoke-static {v2, v1, v4}, Lcom/miui/contentcatcher/service/config/a;->l(Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;)V

    const-string v4, "activityidentity"

    invoke-static {v2, v1, v4}, Lcom/miui/contentcatcher/service/config/a;->l(Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;)V

    const-string v4, "ismultiscreen"

    invoke-static {v2, v1, v4}, Lcom/miui/contentcatcher/service/config/a;->l(Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;)V

    goto :goto_3

    :cond_8
    sget-boolean p0, Lp/b;->a:Z

    if-eqz p0, :cond_9

    invoke-virtual {v3}, Ljava/util/HashMap;->entrySet()Ljava/util/Set;

    move-result-object p0

    invoke-interface {p0}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :goto_4
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_9

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/util/Map$Entry;

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v4, "target:"

    invoke-direct {v2, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-interface {v1}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/String;

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v4, " ClientCatcherResult : "

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v1

    invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v0, v1}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_4

    :cond_9
    return-object v3

    :cond_a
    :goto_5
    const/4 p0, 0x0

    return-object p0
.end method

.method public static l(Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;)V
    .locals 1

    invoke-interface {p0, p2}, Ljava/util/Map;->containsKey(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-interface {p0, p2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    instance-of v0, p0, Ljava/lang/String;

    if-eqz v0, :cond_0

    check-cast p0, Ljava/lang/String;

    invoke-interface {p1, p2, p0}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :cond_0
    return-void
.end method

.method public static m(Lcom/miui/contentcatcher/service/config/a$c;Z)Z
    .locals 2

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "updateTargetState targetInfo:"

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Lcom/miui/contentcatcher/service/config/a$c;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, " enable: "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "ConfigHelper"

    invoke-static {v1, v0}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    iget-boolean v0, p0, Lcom/miui/contentcatcher/service/config/a$c;->a:Z

    if-eq v0, p1, :cond_0

    iput-boolean p1, p0, Lcom/miui/contentcatcher/service/config/a$c;->a:Z

    invoke-virtual {p0}, Lcom/miui/contentcatcher/service/config/a$c;->a()V

    const/4 v0, 0x1

    goto :goto_0

    :cond_0
    const/4 v0, 0x0

    :goto_0
    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/a$c;->g:Ljava/util/ArrayList;

    invoke-virtual {p0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :goto_1
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_1

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lmiui/contentcatcher/sdk/data/FeatureInfo;

    iput-boolean p1, v1, Lmiui/contentcatcher/sdk/data/FeatureInfo;->enable:Z

    goto :goto_1

    :cond_1
    return v0
.end method


# virtual methods
.method public final b(Ljava/lang/String;Lmiui/contentcatcher/sdk/ClientCatcherResult;Z)Landroid/content/Intent;
    .locals 4

    iget-object v0, p0, Lcom/miui/contentcatcher/service/config/a;->c:Ljava/util/ArrayList;

    invoke-virtual {v0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :cond_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_7

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lcom/miui/contentcatcher/service/config/a$c;

    iget-object v1, v1, Lcom/miui/contentcatcher/service/config/a$c;->b:Ljava/lang/String;

    invoke-virtual {p1, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_0

    :try_start_0
    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/a;->a:Landroid/content/Context;

    invoke-virtual {p0}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object p0

    const/4 v0, 0x0

    invoke-virtual {p0, p1, v0}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object p0

    if-nez p0, :cond_1

    goto/16 :goto_4

    :cond_1
    const-string v1, "com.xiaomi.o2o"

    invoke-virtual {v1, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    const/4 v2, 0x1

    if-nez v1, :cond_3

    const-string v1, "com.xiaomi.market"

    invoke-virtual {v1, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_2

    goto :goto_0

    :cond_2
    move v1, v0

    goto :goto_1

    :catch_0
    move-exception p0

    goto :goto_3

    :cond_3
    :goto_0
    const-string v1, "com.miui.contentcatcher.permission.ACCESS_CATCHER"

    iget-object v3, p0, Landroid/content/pm/PackageInfo;->applicationInfo:Landroid/content/pm/ApplicationInfo;

    iget v3, v3, Landroid/content/pm/ApplicationInfo;->uid:I

    invoke-static {v3}, Landroid/os/UserHandle;->getUserId(I)I

    move-result v3

    invoke-static {v1, p1, v3}, Lx/a;->a(Ljava/lang/String;Ljava/lang/String;I)I

    move-result v1

    if-nez v1, :cond_2

    move v1, v2

    :goto_1
    const-string v3, "android.permission.SET_ACTIVITY_WATCHER"

    iget-object p0, p0, Landroid/content/pm/PackageInfo;->applicationInfo:Landroid/content/pm/ApplicationInfo;

    iget p0, p0, Landroid/content/pm/ApplicationInfo;->uid:I

    invoke-static {p0}, Landroid/os/UserHandle;->getUserId(I)I

    move-result p0

    invoke-static {v3, p1, p0}, Lx/a;->a(Ljava/lang/String;Ljava/lang/String;I)I

    move-result p0
    :try_end_0
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_0 .. :try_end_0} :catch_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1

    if-nez p0, :cond_4

    move v0, v2

    :cond_4
    if-nez v1, :cond_5

    if-eqz v0, :cond_7

    :cond_5
    new-instance p0, Landroid/content/Intent;

    invoke-direct {p0}, Landroid/content/Intent;-><init>()V

    if-eqz p3, :cond_6

    const-string p3, "miui.intent.action.TEXT_CONTENT_EXTENSION"

    invoke-virtual {p0, p3}, Landroid/content/Intent;->setAction(Ljava/lang/String;)Landroid/content/Intent;

    goto :goto_2

    :cond_6
    const-string p3, "miui.intent.action.contentcatcher"

    invoke-virtual {p0, p3}, Landroid/content/Intent;->setAction(Ljava/lang/String;)Landroid/content/Intent;

    const/high16 p3, 0x10000000

    invoke-virtual {p0, p3}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;

    :goto_2
    invoke-virtual {p0, p1}, Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;

    const/16 p1, 0x20

    invoke-virtual {p0, p1}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;

    const-string p1, "content"

    invoke-virtual {p0, p1, p2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Landroid/os/Parcelable;)Landroid/content/Intent;

    return-object p0

    :goto_3
    new-instance p1, Ljava/lang/StringBuilder;

    const-string p2, " isValidTarget  e :"

    invoke-direct {p1, p2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {p1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string p1, "ConfigHelper"

    invoke-static {p1, p0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    :catch_1
    :cond_7
    :goto_4
    const/4 p0, 0x0

    return-object p0
.end method

.method public final c(Ljava/lang/String;Ljava/lang/String;ILjava/util/ArrayList;)V
    .locals 11

    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    iget-object v1, p0, Lcom/miui/contentcatcher/service/config/a;->c:Ljava/util/ArrayList;

    invoke-virtual {v1}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :cond_0
    :goto_0
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_a

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lcom/miui/contentcatcher/service/config/a$c;

    iget-boolean v3, v2, Lcom/miui/contentcatcher/service/config/a$c;->a:Z

    if-eqz v3, :cond_0

    iget v3, v2, Lcom/miui/contentcatcher/service/config/a$c;->d:I

    sget v4, Landroid/os/Build$VERSION;->SDK_INT:I

    if-gt v3, v4, :cond_0

    iget v3, v2, Lcom/miui/contentcatcher/service/config/a$c;->e:I

    if-lt v3, v4, :cond_0

    const-string v3, "com.xiaomi.o2o"

    invoke-virtual {v3, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_1

    const-string v3, "com.xiaomi.market"

    invoke-virtual {v3, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_2

    :cond_1
    invoke-virtual {p0, v2}, Lcom/miui/contentcatcher/service/config/a;->f(Lcom/miui/contentcatcher/service/config/a$c;)Z

    move-result v3

    if-nez v3, :cond_2

    goto :goto_0

    :cond_2
    iget-object v2, v2, Lcom/miui/contentcatcher/service/config/a$c;->f:Ljava/util/HashMap;

    invoke-virtual {v2, p1}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lcom/miui/contentcatcher/service/config/a$b;

    if-eqz v2, :cond_0

    iget-object v2, v2, Lcom/miui/contentcatcher/service/config/a$b;->a:Ljava/util/ArrayList;

    if-eqz v2, :cond_0

    invoke-virtual {v2}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v3

    if-nez v3, :cond_0

    invoke-virtual {v2}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v2

    :cond_3
    :goto_1
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    move-result v3

    if-eqz v3, :cond_0

    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Lcom/miui/contentcatcher/service/config/a$a;

    iget-object v4, v3, Lcom/miui/contentcatcher/service/config/a$a;->a:Ljava/lang/String;

    if-eqz v4, :cond_3

    invoke-virtual {v4, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-nez v4, :cond_4

    iget-object v4, v3, Lcom/miui/contentcatcher/service/config/a$a;->a:Ljava/lang/String;

    invoke-virtual {p2, v4}, Ljava/lang/String;->matches(Ljava/lang/String;)Z

    move-result v4

    if-nez v4, :cond_4

    goto :goto_1

    :cond_4
    iget v4, v3, Lcom/miui/contentcatcher/service/config/a$a;->b:I

    if-lt p3, v4, :cond_5

    iget v4, v3, Lcom/miui/contentcatcher/service/config/a$a;->c:I

    :cond_5
    iget-object v4, v3, Lcom/miui/contentcatcher/service/config/a$a;->a:Ljava/lang/String;

    invoke-virtual {v0, v4}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_9

    iget-object v4, v3, Lcom/miui/contentcatcher/service/config/a$a;->a:Ljava/lang/String;

    invoke-virtual {v0, v4}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/util/ArrayList;

    iget-object v3, v3, Lcom/miui/contentcatcher/service/config/a$a;->d:Ljava/util/ArrayList;

    invoke-virtual {v3}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v3

    :cond_6
    :goto_2
    invoke-interface {v3}, Ljava/util/Iterator;->hasNext()Z

    move-result v5

    if-eqz v5, :cond_3

    invoke-interface {v3}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Lmiui/contentcatcher/sdk/data/CatcherInfo;

    invoke-virtual {v4}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v6

    const/4 v7, 0x0

    :cond_7
    :goto_3
    invoke-interface {v6}, Ljava/util/Iterator;->hasNext()Z

    move-result v8

    if-eqz v8, :cond_8

    invoke-interface {v6}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v8

    check-cast v8, Lmiui/contentcatcher/sdk/data/CatcherInfo;

    iget-object v9, v8, Lmiui/contentcatcher/sdk/data/CatcherInfo;->name:Ljava/lang/String;

    iget-object v10, v5, Lmiui/contentcatcher/sdk/data/CatcherInfo;->name:Ljava/lang/String;

    invoke-virtual {v9, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v9

    if-eqz v9, :cond_7

    invoke-virtual {v8, v5}, Lmiui/contentcatcher/sdk/data/CatcherInfo;->merge(Lmiui/contentcatcher/sdk/data/CatcherInfo;)V

    const/4 v7, 0x1

    goto :goto_3

    :cond_8
    if-nez v7, :cond_6

    invoke-virtual {v4, v5}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_2

    :cond_9
    iget-object v4, v3, Lcom/miui/contentcatcher/service/config/a$a;->a:Ljava/lang/String;

    iget-object v3, v3, Lcom/miui/contentcatcher/service/config/a$a;->d:Ljava/util/ArrayList;

    invoke-virtual {v0, v4, v3}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    goto :goto_1

    :cond_a
    invoke-virtual {p4}, Ljava/util/ArrayList;->clear()V

    invoke-virtual {v0, p2}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Ljava/util/ArrayList;

    if-eqz p0, :cond_b

    invoke-virtual {p0}, Ljava/util/ArrayList;->isEmpty()Z

    move-result p1

    if-nez p1, :cond_b

    invoke-virtual {p4, p0}, Ljava/util/ArrayList;->addAll(Ljava/util/Collection;)Z

    :cond_b
    return-void
.end method

.method public final d(Ljava/lang/String;Ljava/lang/String;ILjava/util/ArrayList;)V
    .locals 11
    .annotation build Landroid/annotation/TargetApi;
        value = 0x11
    .end annotation

    invoke-virtual {p4}, Ljava/util/ArrayList;->clear()V

    iget-object v0, p0, Lcom/miui/contentcatcher/service/config/a;->c:Ljava/util/ArrayList;

    invoke-virtual {v0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v0

    const/4 v1, 0x0

    :cond_0
    :goto_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_f

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lcom/miui/contentcatcher/service/config/a$c;

    iget-boolean v3, v2, Lcom/miui/contentcatcher/service/config/a$c;->a:Z

    if-eqz v3, :cond_0

    iget v3, v2, Lcom/miui/contentcatcher/service/config/a$c;->d:I

    sget v4, Landroid/os/Build$VERSION;->SDK_INT:I

    if-gt v3, v4, :cond_0

    iget v3, v2, Lcom/miui/contentcatcher/service/config/a$c;->e:I

    if-lt v3, v4, :cond_0

    const-string v3, "com.xiaomi.o2o"

    invoke-virtual {v3, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_1

    const-string v3, "com.xiaomi.market"

    invoke-virtual {v3, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_2

    :cond_1
    invoke-virtual {p0, v2}, Lcom/miui/contentcatcher/service/config/a;->f(Lcom/miui/contentcatcher/service/config/a$c;)Z

    move-result v3

    if-nez v3, :cond_2

    goto :goto_0

    :cond_2
    iget-object v3, v2, Lcom/miui/contentcatcher/service/config/a$c;->g:Ljava/util/ArrayList;

    if-eqz v3, :cond_0

    invoke-virtual {v3}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v4

    if-nez v4, :cond_0

    invoke-virtual {v3}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v3

    :cond_3
    :goto_1
    invoke-interface {v3}, Ljava/util/Iterator;->hasNext()Z

    move-result v4

    if-eqz v4, :cond_0

    invoke-interface {v3}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lmiui/contentcatcher/sdk/data/FeatureInfo;

    sget-boolean v5, Lp/b;->a:Z

    const-string v6, "ConfigHelper"

    if-eqz v5, :cond_4

    new-instance v5, Ljava/lang/StringBuilder;

    const-string v7, "findFeatures:"

    invoke-direct {v5, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v7, v2, Lcom/miui/contentcatcher/service/config/a$c;->c:Ljava/lang/String;

    invoke-virtual {v5, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v7, " "

    invoke-virtual {v5, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-static {v6, v5}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    :cond_4
    iget-boolean v5, v4, Lmiui/contentcatcher/sdk/data/FeatureInfo;->enable:Z

    if-eqz v5, :cond_3

    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v5, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v7, "/"

    invoke-virtual {v5, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v4}, Lmiui/contentcatcher/sdk/data/FeatureInfo;->getWhiteList()Ljava/util/Set;

    move-result-object v7

    const/4 v8, 0x1

    const-string v9, "fav_mode"

    if-eqz v7, :cond_5

    invoke-interface {v7}, Ljava/util/Set;->isEmpty()Z

    move-result v10

    if-nez v10, :cond_5

    invoke-interface {v7, v5}, Ljava/util/Set;->contains(Ljava/lang/Object;)Z

    move-result v5

    goto :goto_3

    :cond_5
    invoke-virtual {v4}, Lmiui/contentcatcher/sdk/data/FeatureInfo;->getBlackList()Ljava/util/Set;

    move-result-object v7

    if-eqz v7, :cond_6

    invoke-interface {v7}, Ljava/util/Set;->isEmpty()Z

    move-result v10

    if-nez v10, :cond_6

    invoke-interface {v7, v5}, Ljava/util/Set;->contains(Ljava/lang/Object;)Z

    move-result v5

    xor-int/2addr v5, v8

    goto :goto_3

    :cond_6
    iget-object v7, v4, Lmiui/contentcatcher/sdk/data/FeatureInfo;->mParams:Ljava/util/HashMap;

    const-string v10, "extra"

    invoke-virtual {v7, v10}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z

    move-result v7

    if-eqz v7, :cond_7

    iget-object v7, v4, Lmiui/contentcatcher/sdk/data/FeatureInfo;->mParams:Ljava/util/HashMap;

    invoke-virtual {v7, v10}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Ljava/util/Map;

    invoke-interface {v7, v5}, Ljava/util/Map;->containsKey(Ljava/lang/Object;)Z

    move-result v5

    goto :goto_2

    :cond_7
    move v5, v8

    :goto_2
    if-nez v5, :cond_8

    iget-object v7, v4, Lmiui/contentcatcher/sdk/data/FeatureInfo;->name:Ljava/lang/String;

    invoke-virtual {v9, v7}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v7

    if-eqz v7, :cond_8

    const-string v7, "com.tencent.mm"

    invoke-virtual {v7, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v7

    if-eqz v7, :cond_8

    invoke-static {p2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v7

    if-nez v7, :cond_8

    const-string v7, "webview"

    invoke-virtual {p2, v7}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v7

    if-eqz v7, :cond_8

    move v5, v8

    :cond_8
    :goto_3
    if-nez v5, :cond_9

    goto/16 :goto_1

    :cond_9
    iget-object v5, v4, Lmiui/contentcatcher/sdk/data/FeatureInfo;->name:Ljava/lang/String;

    invoke-virtual {v9, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_a

    iget-object v5, p0, Lcom/miui/contentcatcher/service/config/a;->a:Landroid/content/Context;

    if-eqz v5, :cond_3

    invoke-virtual {v5}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v5

    new-instance v6, Ljava/lang/StringBuilder;

    const-string v7, "com.miui.personalassistant.preferences.catcher_"

    invoke-direct {v6, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v6, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v6

    invoke-static {v5, v6, v8}, Landroid/provider/Settings$Global;->getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I

    move-result v5

    if-ne v5, v8, :cond_3

    invoke-virtual {p4, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto/16 :goto_1

    :cond_a
    const-string v5, "pick_mode"

    iget-object v7, v4, Lmiui/contentcatcher/sdk/data/FeatureInfo;->name:Ljava/lang/String;

    invoke-virtual {v5, v7}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_e

    new-instance v5, Ljava/lang/StringBuilder;

    const-string v7, "retrieveReflectionInfo:"

    invoke-direct {v5, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v5, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v7, " activityName:"

    invoke-virtual {v5, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v7, " version:"

    invoke-virtual {v5, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5, p3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-static {v6, v5}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v5, p0, Lcom/miui/contentcatcher/service/config/a;->f:Lcom/miui/contentcatcher/service/config/ViewConfig;

    if-nez v5, :cond_b

    invoke-virtual {p0}, Lcom/miui/contentcatcher/service/config/a;->h()V

    :cond_b
    new-instance v5, Ljava/util/HashMap;

    invoke-direct {v5}, Ljava/util/HashMap;-><init>()V

    iget-object v7, p0, Lcom/miui/contentcatcher/service/config/a;->f:Lcom/miui/contentcatcher/service/config/ViewConfig;

    invoke-virtual {v7, p1}, Lcom/miui/contentcatcher/service/config/ViewConfig;->getPackage(Ljava/lang/String;)Lcom/miui/contentcatcher/service/config/ViewConfig$Package;

    move-result-object v7

    if-eqz v7, :cond_c

    invoke-virtual {v7, p3}, Lcom/miui/contentcatcher/service/config/ViewConfig$Package;->getConfig(I)Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;

    move-result-object v7

    if-eqz v7, :cond_c

    invoke-virtual {v7, p2}, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;->getCatcher(Ljava/lang/String;)Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;

    move-result-object v7

    if-eqz v7, :cond_c

    invoke-virtual {v7}, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;->getViews()Ljava/util/List;

    move-result-object v7

    const/4 v8, 0x0

    :goto_4
    invoke-interface {v7}, Ljava/util/List;->size()I

    move-result v9

    if-ge v8, v9, :cond_c

    invoke-interface {v7, v8}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v9

    check-cast v9, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher$View;

    invoke-virtual {v9}, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher$View;->getType()Ljava/lang/String;

    move-result-object v9

    invoke-interface {v7, v8}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v10

    check-cast v10, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher$View;

    invoke-virtual {v10}, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher$View;->getName()Ljava/lang/String;

    move-result-object v10

    invoke-virtual {v5, v9, v10}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    add-int/lit8 v8, v8, 0x1

    goto :goto_4

    :cond_c
    invoke-virtual {v5}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-static {v6, v7}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v6, v4, Lmiui/contentcatcher/sdk/data/FeatureInfo;->mParams:Ljava/util/HashMap;

    const-string v7, "reflectioninfo"

    invoke-virtual {v6, v7, v5}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    iget-object v5, v2, Lcom/miui/contentcatcher/service/config/a$c;->c:Ljava/lang/String;

    const-string v6, "recomment"

    invoke-virtual {v6, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_d

    if-nez v1, :cond_3

    :cond_d
    move-object v1, v4

    goto/16 :goto_1

    :cond_e
    invoke-virtual {p4, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto/16 :goto_1

    :cond_f
    if-eqz v1, :cond_10

    invoke-virtual {p4, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_10
    return-void
.end method

.method public final f(Lcom/miui/contentcatcher/service/config/a$c;)Z
    .locals 2

    const/4 v0, 0x0

    :try_start_0
    iget-object v1, p1, Lcom/miui/contentcatcher/service/config/a$c;->b:Ljava/lang/String;

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/a;->a:Landroid/content/Context;

    invoke-virtual {p0}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object p0

    invoke-virtual {p0, v1, v0}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object p0

    if-nez p0, :cond_1

    iget-boolean p0, p1, Lcom/miui/contentcatcher/service/config/a$c;->a:Z

    if-eqz p0, :cond_0

    sget-boolean p0, Lp/b;->a:Z

    if-nez p0, :cond_0

    iput-boolean v0, p1, Lcom/miui/contentcatcher/service/config/a$c;->a:Z

    invoke-virtual {p1}, Lcom/miui/contentcatcher/service/config/a$c;->a()V
    :try_end_0
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_0 .. :try_end_0} :catch_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    :cond_0
    return v0

    :cond_1
    const/4 p0, 0x1

    return p0

    :catch_0
    return v0
.end method

.method public final g(Lcom/miui/contentcatcher/common/ExtraConfigInfo;)Z
    .locals 1

    iget-object v0, p1, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->b:Ljava/lang/String;

    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v0

    if-nez v0, :cond_2

    iget-object v0, p1, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->c:Ljava/lang/String;

    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v0

    if-eqz v0, :cond_0

    iget-object v0, p1, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->d:Ljava/lang/String;

    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v0

    if-eqz v0, :cond_0

    iget-object v0, p1, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->i:Ljava/lang/String;

    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v0

    if-nez v0, :cond_2

    :cond_0
    iget-object v0, p1, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->p:Ljava/lang/String;

    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v0

    if-eqz v0, :cond_1

    iget-object v0, p1, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->q:Ljava/lang/String;

    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v0

    if-eqz v0, :cond_1

    iget-object p1, p1, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->r:Ljava/lang/String;

    invoke-static {p1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result p1

    if-eqz p1, :cond_1

    goto :goto_0

    :cond_1
    const/4 p0, 0x1

    return p0

    :cond_2
    :goto_0
    new-instance p1, Ljava/lang/StringBuilder;

    const-string v0, "isValid(): false oldVersion for "

    invoke-direct {p1, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {p1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string p1, "ConfigHelper"

    invoke-static {p1, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    const/4 p0, 0x0

    return p0
.end method

.method public final h()V
    .locals 10

    iget-object v0, p0, Lcom/miui/contentcatcher/service/config/a;->a:Landroid/content/Context;

    const-string v1, "ViewConfigParser"

    const-string v2, "parseConfigFile : "

    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v3

    const/4 v5, 0x0

    :try_start_0
    invoke-virtual {v0}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;

    move-result-object v0

    const-string v6, "config/config_view.json"

    invoke-virtual {v0, v6}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;

    move-result-object v0

    new-instance v6, Ljava/io/BufferedReader;

    new-instance v7, Ljava/io/InputStreamReader;

    invoke-direct {v7, v0}, Ljava/io/InputStreamReader;-><init>(Ljava/io/InputStream;)V

    invoke-direct {v6, v7}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_3
    .catchall {:try_start_0 .. :try_end_0} :catchall_1

    :try_start_1
    new-instance v0, Lcom/google/gson/d;

    invoke-direct {v0}, Lcom/google/gson/d;-><init>()V

    const-class v7, Lcom/miui/contentcatcher/service/config/ViewConfig;

    new-instance v8, Li/a;

    invoke-direct {v8, v7}, Li/a;-><init>(Ljava/lang/reflect/Type;)V

    invoke-virtual {v0, v6, v8}, Lcom/google/gson/d;->b(Ljava/io/Reader;Li/a;)Ljava/lang/Object;

    move-result-object v0

    invoke-static {v7}, Ld/a;->d(Ljava/lang/Class;)Ljava/lang/Class;

    move-result-object v7

    invoke-virtual {v7, v0}, Ljava/lang/Class;->cast(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcom/miui/contentcatcher/service/config/ViewConfig;
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_2
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :try_start_2
    invoke-virtual {v0}, Lcom/miui/contentcatcher/service/config/ViewConfig;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-static {v1, v5}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_1
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    :try_start_3
    invoke-virtual {v6}, Ljava/io/BufferedReader;->close()V
    :try_end_3
    .catch Ljava/io/IOException; {:try_start_3 .. :try_end_3} :catch_0

    goto :goto_2

    :catch_0
    move-exception v1

    invoke-virtual {v1}, Ljava/lang/Throwable;->printStackTrace()V

    goto :goto_2

    :catchall_0
    move-exception p0

    move-object v5, v6

    goto :goto_3

    :catch_1
    move-exception v5

    goto :goto_0

    :catch_2
    move-exception v0

    move-object v9, v5

    move-object v5, v0

    move-object v0, v9

    goto :goto_0

    :catchall_1
    move-exception p0

    goto :goto_3

    :catch_3
    move-exception v0

    move-object v6, v5

    move-object v5, v0

    move-object v0, v6

    :goto_0
    :try_start_4
    new-instance v7, Ljava/lang/StringBuilder;

    invoke-direct {v7, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v7, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    if-eqz v6, :cond_0

    :try_start_5
    invoke-virtual {v6}, Ljava/io/BufferedReader;->close()V
    :try_end_5
    .catch Ljava/io/IOException; {:try_start_5 .. :try_end_5} :catch_4

    goto :goto_1

    :catch_4
    move-exception v2

    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_0
    :goto_1
    new-instance v2, Ljava/lang/StringBuilder;

    const-string v5, "parseConfigFile(): using "

    invoke-direct {v2, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v5

    sub-long/2addr v5, v3

    const-wide/32 v3, 0xf4240

    div-long/2addr v5, v3

    invoke-virtual {v2, v5, v6}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    const-string v3, " ms"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->i(Ljava/lang/String;Ljava/lang/String;)V

    :goto_2
    iput-object v0, p0, Lcom/miui/contentcatcher/service/config/a;->f:Lcom/miui/contentcatcher/service/config/ViewConfig;

    return-void

    :goto_3
    if-eqz v5, :cond_1

    :try_start_6
    invoke-virtual {v5}, Ljava/io/BufferedReader;->close()V
    :try_end_6
    .catch Ljava/io/IOException; {:try_start_6 .. :try_end_6} :catch_5

    goto :goto_4

    :catch_5
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_1
    :goto_4
    throw p0
.end method

.method public final i(Landroid/util/JsonReader;)V
    .locals 21
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    move-object/from16 v1, p0

    const-string v0, ""

    iput-object v0, v1, Lcom/miui/contentcatcher/service/config/a;->d:Ljava/lang/String;

    iput-object v0, v1, Lcom/miui/contentcatcher/service/config/a;->e:Ljava/lang/String;

    new-instance v2, Lcom/miui/contentcatcher/service/config/a$c;

    invoke-direct {v2, v1}, Lcom/miui/contentcatcher/service/config/a$c;-><init>(Lcom/miui/contentcatcher/service/config/a;)V

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->beginObject()V

    const/4 v0, 0x0

    :goto_0
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->hasNext()Z

    move-result v4

    const-string v5, "#"

    const-string v6, "ConfigHelper"

    if-eqz v4, :cond_2e

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextName()Ljava/lang/String;

    move-result-object v4

    const-string v7, "min_sdk_version"

    invoke-virtual {v7, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v7

    if-eqz v7, :cond_0

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextInt()I

    move-result v4

    iput v4, v2, Lcom/miui/contentcatcher/service/config/a$c;->d:I

    goto :goto_1

    :cond_0
    const-string v7, "max_sdk_version"

    invoke-virtual {v7, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v7

    if-eqz v7, :cond_1

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextInt()I

    move-result v4

    iput v4, v2, Lcom/miui/contentcatcher/service/config/a$c;->e:I

    goto :goto_1

    :cond_1
    const-string v7, "target"

    invoke-virtual {v7, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v7

    if-eqz v7, :cond_2

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextString()Ljava/lang/String;

    move-result-object v4

    iput-object v4, v1, Lcom/miui/contentcatcher/service/config/a;->d:Ljava/lang/String;

    iput-object v4, v2, Lcom/miui/contentcatcher/service/config/a$c;->b:Ljava/lang/String;

    goto :goto_1

    :cond_2
    const-string v7, "job_tag"

    invoke-virtual {v7, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v7

    if-eqz v7, :cond_3

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextString()Ljava/lang/String;

    move-result-object v4

    iput-object v4, v1, Lcom/miui/contentcatcher/service/config/a;->e:Ljava/lang/String;

    iput-object v4, v2, Lcom/miui/contentcatcher/service/config/a$c;->c:Ljava/lang/String;

    goto :goto_1

    :cond_3
    const-string v7, "config_version"

    invoke-virtual {v7, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v7

    if-eqz v7, :cond_6

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->peek()Landroid/util/JsonToken;

    move-result-object v4

    sget-object v5, Landroid/util/JsonToken;->NUMBER:Landroid/util/JsonToken;

    if-ne v4, v5, :cond_4

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextInt()I

    goto :goto_1

    :cond_4
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->skipValue()V

    :cond_5
    :goto_1
    move-object/from16 v19, v2

    goto/16 :goto_23

    :cond_6
    const-string v7, "packages"

    invoke-virtual {v7, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v7

    const-string v8, "name"

    if-eqz v7, :cond_12

    iget-object v4, v2, Lcom/miui/contentcatcher/service/config/a$c;->f:Ljava/util/HashMap;

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->beginArray()V

    :goto_2
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->hasNext()Z

    move-result v5

    if-eqz v5, :cond_11

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->beginObject()V

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextName()Ljava/lang/String;

    move-result-object v5

    new-instance v7, Ljava/lang/StringBuilder;

    const-string v9, "parse package : "

    invoke-direct {v7, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v7, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-static {v6, v7}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v7, Lcom/miui/contentcatcher/service/config/a$b;

    invoke-direct {v7}, Lcom/miui/contentcatcher/service/config/a$b;-><init>()V

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->beginArray()V

    :goto_3
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->hasNext()Z

    move-result v9

    if-eqz v9, :cond_10

    new-instance v9, Lcom/miui/contentcatcher/service/config/a$a;

    invoke-direct {v9}, Lcom/miui/contentcatcher/service/config/a$a;-><init>()V

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->beginObject()V

    :cond_7
    :goto_4
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->hasNext()Z

    move-result v10

    if-eqz v10, :cond_f

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextName()Ljava/lang/String;

    move-result-object v10

    const-string v11, "activity"

    invoke-virtual {v11, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v11

    if-eqz v11, :cond_8

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextString()Ljava/lang/String;

    move-result-object v10

    iput-object v10, v9, Lcom/miui/contentcatcher/service/config/a$a;->a:Ljava/lang/String;

    goto :goto_4

    :cond_8
    const-string v11, "min_version"

    invoke-virtual {v11, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v11

    if-eqz v11, :cond_9

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextInt()I

    move-result v10

    iput v10, v9, Lcom/miui/contentcatcher/service/config/a$a;->b:I

    goto :goto_4

    :cond_9
    const-string v11, "max_version"

    invoke-virtual {v11, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v11

    if-eqz v11, :cond_a

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextInt()I

    move-result v10

    iput v10, v9, Lcom/miui/contentcatcher/service/config/a$a;->c:I

    goto :goto_4

    :cond_a
    const-string v11, "catchers"

    invoke-virtual {v11, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v10

    if-eqz v10, :cond_7

    new-instance v10, Ljava/util/ArrayList;

    invoke-direct {v10}, Ljava/util/ArrayList;-><init>()V

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->beginArray()V

    :goto_5
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->hasNext()Z

    move-result v11

    if-eqz v11, :cond_e

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->beginObject()V

    const-string v11, "catcher"

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextName()Ljava/lang/String;

    move-result-object v12

    invoke-virtual {v11, v12}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v11

    if-eqz v11, :cond_d

    new-instance v11, Lmiui/contentcatcher/sdk/data/CatcherInfo;

    invoke-direct {v11}, Lmiui/contentcatcher/sdk/data/CatcherInfo;-><init>()V

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->beginObject()V

    :goto_6
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->hasNext()Z

    move-result v12

    if-eqz v12, :cond_c

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextName()Ljava/lang/String;

    move-result-object v12

    invoke-virtual {v8, v12}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v13

    if-eqz v13, :cond_b

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextString()Ljava/lang/String;

    move-result-object v12

    iput-object v12, v11, Lmiui/contentcatcher/sdk/data/CatcherInfo;->name:Ljava/lang/String;

    goto :goto_6

    :cond_b
    iget-object v13, v1, Lcom/miui/contentcatcher/service/config/a;->d:Ljava/lang/String;

    iget-object v14, v1, Lcom/miui/contentcatcher/service/config/a;->e:Ljava/lang/String;

    invoke-static {v13, v14, v12}, Lp/a;->a(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v12

    iget-object v13, v11, Lmiui/contentcatcher/sdk/data/CatcherInfo;->mParams:Ljava/util/Map;

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextString()Ljava/lang/String;

    move-result-object v14

    invoke-interface {v13, v12, v14}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    goto :goto_6

    :cond_c
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->endObject()V

    new-instance v12, Ljava/lang/StringBuilder;

    const-string v13, "parse catcher: "

    invoke-direct {v12, v13}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v11}, Lmiui/contentcatcher/sdk/data/CatcherInfo;->toString()Ljava/lang/String;

    move-result-object v13

    invoke-virtual {v12, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v12}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v12

    invoke-static {v6, v12}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v10, v11}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_d
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->endObject()V

    goto :goto_5

    :cond_e
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->endArray()V

    iput-object v10, v9, Lcom/miui/contentcatcher/service/config/a$a;->d:Ljava/util/ArrayList;

    goto/16 :goto_4

    :cond_f
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->endObject()V

    new-instance v10, Ljava/lang/StringBuilder;

    const-string v11, "parseActivity: "

    invoke-direct {v10, v11}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v9}, Lcom/miui/contentcatcher/service/config/a$a;->toString()Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v10

    invoke-static {v6, v10}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v10, v7, Lcom/miui/contentcatcher/service/config/a$b;->a:Ljava/util/ArrayList;

    invoke-virtual {v10, v9}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto/16 :goto_3

    :cond_10
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->endArray()V

    invoke-virtual {v4, v5, v7}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    new-instance v9, Ljava/lang/StringBuilder;

    const-string v10, "packageMap add package : "

    invoke-direct {v9, v10}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v9, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v5, " packageInfo : "

    invoke-virtual {v9, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v9, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v9}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-static {v6, v5}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->endObject()V

    goto/16 :goto_2

    :cond_11
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->endArray()V

    goto/16 :goto_1

    :cond_12
    const-string v7, "features"

    invoke-virtual {v7, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_5

    iget-object v4, v2, Lcom/miui/contentcatcher/service/config/a$c;->g:Ljava/util/ArrayList;

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->beginArray()V

    const/4 v7, 0x0

    :goto_7
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_2d

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->beginObject()V

    const-string v0, "feature"

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextName()Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v0, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_2c

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->beginObject()V

    new-instance v9, Lmiui/contentcatcher/sdk/data/FeatureInfo;

    invoke-direct {v9}, Lmiui/contentcatcher/sdk/data/FeatureInfo;-><init>()V

    iget-object v0, v1, Lcom/miui/contentcatcher/service/config/a;->d:Ljava/lang/String;

    iput-object v0, v9, Lmiui/contentcatcher/sdk/data/FeatureInfo;->target:Ljava/lang/String;

    iget-object v0, v1, Lcom/miui/contentcatcher/service/config/a;->e:Ljava/lang/String;

    iput-object v0, v9, Lmiui/contentcatcher/sdk/data/FeatureInfo;->jobTag:Ljava/lang/String;

    :goto_8
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_2a

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextName()Ljava/lang/String;

    move-result-object v10

    invoke-virtual {v8, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_13

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, v9, Lmiui/contentcatcher/sdk/data/FeatureInfo;->name:Ljava/lang/String;

    :goto_9
    move-object/from16 v19, v2

    move-object/from16 v20, v4

    move/from16 v17, v7

    move-object/from16 v18, v8

    goto/16 :goto_21

    :cond_13
    const-string v0, "enable"

    invoke-virtual {v0, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_14

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextBoolean()Z

    move-result v0

    iput-boolean v0, v9, Lmiui/contentcatcher/sdk/data/FeatureInfo;->enable:Z

    goto :goto_9

    :cond_14
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->nextString()Ljava/lang/String;

    move-result-object v0

    const-string v11, "extra"

    invoke-virtual {v11, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v11

    if-eqz v11, :cond_28

    const-string v11, "fav_mode"

    iget-object v12, v9, Lmiui/contentcatcher/sdk/data/FeatureInfo;->name:Ljava/lang/String;

    invoke-virtual {v11, v12}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v11

    const-string v12, "parseConfigFile(): file missing!"

    const-string v3, "parseConfigFile(): using "

    const-string v13, " ms"

    if-eqz v11, :cond_1f

    iget-object v11, v1, Lcom/miui/contentcatcher/service/config/a;->a:Landroid/content/Context;

    new-instance v14, Ljava/lang/StringBuilder;

    const-string v15, "parseExtraFile(): pid = "

    invoke-direct {v14, v15}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Landroid/os/Process;->myPid()I

    move-result v15

    invoke-virtual {v14, v15}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v14}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v14

    invoke-static {v6, v14}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v14

    move/from16 v17, v7

    new-instance v7, Ljava/util/HashMap;

    invoke-direct {v7}, Ljava/util/HashMap;-><init>()V

    :try_start_0
    invoke-virtual {v11}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;

    move-result-object v11

    invoke-virtual {v11, v0}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;

    move-result-object v11
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_5
    .catchall {:try_start_0 .. :try_end_0} :catchall_2

    move-object/from16 v18, v8

    :try_start_1
    new-instance v8, Ljava/io/BufferedReader;

    new-instance v0, Ljava/io/InputStreamReader;

    invoke-direct {v0, v11}, Ljava/io/InputStreamReader;-><init>(Ljava/io/InputStream;)V

    invoke-direct {v8, v0}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_4
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    :cond_15
    :goto_a
    :try_start_2
    invoke-virtual {v8}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;

    move-result-object v0

    if-eqz v0, :cond_19

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0, v5}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v16

    if-nez v16, :cond_15

    invoke-virtual {v0}, Ljava/lang/String;->isEmpty()Z

    move-result v16

    if-eqz v16, :cond_16

    goto :goto_a

    :cond_16
    invoke-virtual {v1, v0}, Lcom/miui/contentcatcher/service/config/a;->k(Ljava/lang/String;)Lcom/miui/contentcatcher/common/ExtraConfigInfo;

    move-result-object v0
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_1
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    if-eqz v0, :cond_18

    move-object/from16 v19, v2

    :try_start_3
    iget-object v2, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->b:Ljava/lang/String;

    invoke-virtual {v7, v2}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    if-nez v2, :cond_17

    iget-object v2, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->b:Ljava/lang/String;
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_0
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    move-object/from16 v20, v4

    :try_start_4
    new-instance v4, Ljava/util/HashSet;

    invoke-direct {v4}, Ljava/util/HashSet;-><init>()V

    invoke-virtual {v7, v2, v4}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    goto :goto_c

    :catchall_0
    move-exception v0

    move-object v1, v0

    goto/16 :goto_15

    :catch_0
    :goto_b
    move-object/from16 v20, v4

    goto/16 :goto_11

    :cond_17
    move-object/from16 v20, v4

    :goto_c
    new-instance v2, Lcom/google/gson/d;

    invoke-direct {v2}, Lcom/google/gson/d;-><init>()V

    invoke-virtual {v2, v0}, Lcom/google/gson/d;->f(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    iget-object v0, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->b:Ljava/lang/String;

    invoke-virtual {v7, v0}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/util/Set;

    invoke-interface {v0, v2}, Ljava/util/Set;->add(Ljava/lang/Object;)Z
    :try_end_4
    .catch Ljava/lang/Exception; {:try_start_4 .. :try_end_4} :catch_6
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    goto :goto_d

    :cond_18
    move-object/from16 v19, v2

    move-object/from16 v20, v4

    :goto_d
    move-object/from16 v2, v19

    move-object/from16 v4, v20

    goto :goto_a

    :catch_1
    move-object/from16 v19, v2

    goto :goto_b

    :cond_19
    move-object/from16 v19, v2

    move-object/from16 v20, v4

    if-eqz v11, :cond_1a

    :try_start_5
    invoke-virtual {v11}, Ljava/io/InputStream;->close()V
    :try_end_5
    .catch Ljava/io/IOException; {:try_start_5 .. :try_end_5} :catch_2

    goto :goto_e

    :catch_2
    move-exception v0

    move-object v2, v0

    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_1a
    :goto_e
    :try_start_6
    invoke-virtual {v8}, Ljava/io/BufferedReader;->close()V
    :try_end_6
    .catch Ljava/io/IOException; {:try_start_6 .. :try_end_6} :catch_3

    goto :goto_f

    :catch_3
    move-exception v0

    move-object v2, v0

    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    :goto_f
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    :goto_10
    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v2

    sub-long/2addr v2, v14

    const-wide/32 v11, 0xf4240

    div-long/2addr v2, v11

    invoke-virtual {v0, v2, v3}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    invoke-virtual {v0, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v6, v0}, Lp/b;->i(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_14

    :catchall_1
    move-exception v0

    move-object v1, v0

    const/4 v8, 0x0

    goto :goto_15

    :catch_4
    move-object/from16 v19, v2

    move-object/from16 v20, v4

    const/4 v8, 0x0

    goto :goto_11

    :catchall_2
    move-exception v0

    move-object v1, v0

    const/4 v8, 0x0

    const/4 v11, 0x0

    goto :goto_15

    :catch_5
    move-object/from16 v19, v2

    move-object/from16 v20, v4

    move-object/from16 v18, v8

    const/4 v8, 0x0

    const/4 v11, 0x0

    :catch_6
    :goto_11
    :try_start_7
    invoke-static {v6, v12}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_7
    .catchall {:try_start_7 .. :try_end_7} :catchall_0

    if-eqz v11, :cond_1b

    :try_start_8
    invoke-virtual {v11}, Ljava/io/InputStream;->close()V
    :try_end_8
    .catch Ljava/io/IOException; {:try_start_8 .. :try_end_8} :catch_7

    goto :goto_12

    :catch_7
    move-exception v0

    move-object v2, v0

    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_1b
    :goto_12
    if-eqz v8, :cond_1c

    :try_start_9
    invoke-virtual {v8}, Ljava/io/BufferedReader;->close()V
    :try_end_9
    .catch Ljava/io/IOException; {:try_start_9 .. :try_end_9} :catch_8

    goto :goto_13

    :catch_8
    move-exception v0

    move-object v2, v0

    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_1c
    :goto_13
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    goto :goto_10

    :goto_14
    iget-object v0, v9, Lmiui/contentcatcher/sdk/data/FeatureInfo;->mParams:Ljava/util/HashMap;

    invoke-virtual {v0, v10, v7}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    goto/16 :goto_21

    :goto_15
    if-eqz v11, :cond_1d

    :try_start_a
    invoke-virtual {v11}, Ljava/io/InputStream;->close()V
    :try_end_a
    .catch Ljava/io/IOException; {:try_start_a .. :try_end_a} :catch_9

    goto :goto_16

    :catch_9
    move-exception v0

    move-object v2, v0

    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_1d
    :goto_16
    if-eqz v8, :cond_1e

    :try_start_b
    invoke-virtual {v8}, Ljava/io/BufferedReader;->close()V
    :try_end_b
    .catch Ljava/io/IOException; {:try_start_b .. :try_end_b} :catch_a

    goto :goto_17

    :catch_a
    move-exception v0

    move-object v2, v0

    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_1e
    :goto_17
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v2

    sub-long/2addr v2, v14

    const-wide/32 v4, 0xf4240

    div-long/2addr v2, v4

    invoke-virtual {v0, v2, v3}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    invoke-virtual {v0, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v6, v0}, Lp/b;->i(Ljava/lang/String;Ljava/lang/String;)V

    throw v1

    :cond_1f
    move-object/from16 v19, v2

    move-object/from16 v20, v4

    move/from16 v17, v7

    move-object/from16 v18, v8

    const-string v2, "pip_mode"

    iget-object v4, v9, Lmiui/contentcatcher/sdk/data/FeatureInfo;->name:Ljava/lang/String;

    invoke-virtual {v2, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_29

    iget-object v2, v1, Lcom/miui/contentcatcher/service/config/a;->a:Landroid/content/Context;

    new-instance v4, Ljava/lang/StringBuilder;

    const-string v7, "parseConfigFile(): pid = "

    invoke-direct {v4, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Landroid/os/Process;->myPid()I

    move-result v7

    invoke-virtual {v4, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    const-string v7, "contentCatcher:PIPConfig"

    invoke-static {v7, v4}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v14

    new-instance v4, Ljava/util/HashMap;

    invoke-direct {v4}, Ljava/util/HashMap;-><init>()V

    :try_start_c
    invoke-virtual {v2}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;

    move-result-object v2

    invoke-virtual {v2, v0}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;

    move-result-object v2
    :try_end_c
    .catch Ljava/lang/Exception; {:try_start_c .. :try_end_c} :catch_e
    .catchall {:try_start_c .. :try_end_c} :catchall_5

    :try_start_d
    new-instance v8, Ljava/io/BufferedReader;

    new-instance v0, Ljava/io/InputStreamReader;

    invoke-direct {v0, v2}, Ljava/io/InputStreamReader;-><init>(Ljava/io/InputStream;)V

    invoke-direct {v8, v0}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V
    :try_end_d
    .catch Ljava/lang/Exception; {:try_start_d .. :try_end_d} :catch_d
    .catchall {:try_start_d .. :try_end_d} :catchall_4

    :cond_20
    :goto_18
    :try_start_e
    invoke-virtual {v8}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;

    move-result-object v0

    if-eqz v0, :cond_22

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0, v5}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v11

    if-nez v11, :cond_20

    invoke-virtual {v0}, Ljava/lang/String;->isEmpty()Z

    move-result v11

    if-eqz v11, :cond_21

    goto :goto_18

    :cond_21
    invoke-static {v0}, Lcom/miui/contentcatcher/common/PIPConfigInfo;->a(Ljava/lang/String;)Lcom/miui/contentcatcher/common/PIPConfigInfo;

    move-result-object v11

    if-eqz v11, :cond_20

    iget-object v11, v11, Lcom/miui/contentcatcher/common/PIPConfigInfo;->c:Ljava/lang/String;

    invoke-virtual {v4, v11, v0}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_e
    .catch Ljava/lang/Exception; {:try_start_e .. :try_end_e} :catch_f
    .catchall {:try_start_e .. :try_end_e} :catchall_3

    goto :goto_18

    :catchall_3
    move-exception v0

    move-object v1, v0

    move-object v13, v2

    goto :goto_1e

    :cond_22
    if-eqz v2, :cond_23

    :try_start_f
    invoke-virtual {v2}, Ljava/io/InputStream;->close()V
    :try_end_f
    .catch Ljava/io/IOException; {:try_start_f .. :try_end_f} :catch_b

    goto :goto_19

    :catch_b
    move-exception v0

    move-object v2, v0

    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_23
    :goto_19
    :try_start_10
    invoke-virtual {v8}, Ljava/io/BufferedReader;->close()V
    :try_end_10
    .catch Ljava/io/IOException; {:try_start_10 .. :try_end_10} :catch_c

    goto :goto_1d

    :catch_c
    move-exception v0

    move-object v2, v0

    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    goto :goto_1d

    :catchall_4
    move-exception v0

    move-object v1, v0

    move-object v13, v2

    const/4 v8, 0x0

    goto :goto_1e

    :catch_d
    :goto_1a
    const/4 v8, 0x0

    goto :goto_1b

    :catchall_5
    move-exception v0

    move-object v1, v0

    const/4 v8, 0x0

    const/4 v13, 0x0

    goto :goto_1e

    :catch_e
    const/4 v2, 0x0

    goto :goto_1a

    :catch_f
    :goto_1b
    :try_start_11
    invoke-static {v7, v12}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_11
    .catchall {:try_start_11 .. :try_end_11} :catchall_3

    if-eqz v2, :cond_24

    :try_start_12
    invoke-virtual {v2}, Ljava/io/InputStream;->close()V
    :try_end_12
    .catch Ljava/io/IOException; {:try_start_12 .. :try_end_12} :catch_10

    goto :goto_1c

    :catch_10
    move-exception v0

    move-object v2, v0

    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_24
    :goto_1c
    if-eqz v8, :cond_25

    :try_start_13
    invoke-virtual {v8}, Ljava/io/BufferedReader;->close()V
    :try_end_13
    .catch Ljava/io/IOException; {:try_start_13 .. :try_end_13} :catch_c

    :cond_25
    :goto_1d
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v2

    sub-long/2addr v2, v14

    const-wide/32 v11, 0xf4240

    div-long/2addr v2, v11

    invoke-virtual {v0, v2, v3}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    invoke-virtual {v0, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v7, v0}, Lp/b;->i(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v0, v9, Lmiui/contentcatcher/sdk/data/FeatureInfo;->mParams:Ljava/util/HashMap;

    invoke-virtual {v0, v10, v4}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    goto :goto_21

    :goto_1e
    if-eqz v13, :cond_26

    :try_start_14
    invoke-virtual {v13}, Ljava/io/InputStream;->close()V
    :try_end_14
    .catch Ljava/io/IOException; {:try_start_14 .. :try_end_14} :catch_11

    goto :goto_1f

    :catch_11
    move-exception v0

    move-object v2, v0

    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_26
    :goto_1f
    if-eqz v8, :cond_27

    :try_start_15
    invoke-virtual {v8}, Ljava/io/BufferedReader;->close()V
    :try_end_15
    .catch Ljava/io/IOException; {:try_start_15 .. :try_end_15} :catch_12

    goto :goto_20

    :catch_12
    move-exception v0

    move-object v2, v0

    invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_27
    :goto_20
    throw v1

    :cond_28
    move-object/from16 v19, v2

    move-object/from16 v20, v4

    move/from16 v17, v7

    move-object/from16 v18, v8

    iget-object v2, v9, Lmiui/contentcatcher/sdk/data/FeatureInfo;->mParams:Ljava/util/HashMap;

    invoke-virtual {v2, v10, v0}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :cond_29
    :goto_21
    move/from16 v7, v17

    move-object/from16 v8, v18

    move-object/from16 v2, v19

    move-object/from16 v4, v20

    goto/16 :goto_8

    :cond_2a
    move-object/from16 v19, v2

    move-object/from16 v20, v4

    move/from16 v17, v7

    move-object/from16 v18, v8

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->endObject()V

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v2, "parseFeature: "

    invoke-direct {v0, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v6, v0}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    move-object/from16 v2, v20

    invoke-virtual {v2, v9}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    iget-boolean v0, v9, Lmiui/contentcatcher/sdk/data/FeatureInfo;->enable:Z

    if-eqz v0, :cond_2b

    const/4 v7, 0x1

    goto :goto_22

    :cond_2b
    move/from16 v7, v17

    goto :goto_22

    :cond_2c
    move-object/from16 v19, v2

    move-object v2, v4

    move/from16 v17, v7

    move-object/from16 v18, v8

    :goto_22
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->endObject()V

    move-object v4, v2

    move-object/from16 v8, v18

    move-object/from16 v2, v19

    goto/16 :goto_7

    :cond_2d
    move-object/from16 v19, v2

    move/from16 v17, v7

    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->endArray()V

    move/from16 v0, v17

    :goto_23
    move-object/from16 v2, v19

    goto/16 :goto_0

    :cond_2e
    iget-object v3, v2, Lcom/miui/contentcatcher/service/config/a$c;->b:Ljava/lang/String;

    invoke-static {v3}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v3

    if-nez v3, :cond_31

    iget-object v3, v2, Lcom/miui/contentcatcher/service/config/a$c;->h:Lcom/miui/contentcatcher/service/config/a;

    iget-object v3, v3, Lcom/miui/contentcatcher/service/config/a;->b:Landroid/content/SharedPreferences;

    if-eqz v3, :cond_2f

    :try_start_16
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    iget-object v7, v2, Lcom/miui/contentcatcher/service/config/a$c;->b:Ljava/lang/String;

    invoke-virtual {v4, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v5, v2, Lcom/miui/contentcatcher/service/config/a$c;->c:Ljava/lang/String;

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-interface {v3, v4, v0}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0

    iput-boolean v0, v2, Lcom/miui/contentcatcher/service/config/a$c;->a:Z

    invoke-static {v2, v0}, Lcom/miui/contentcatcher/service/config/a;->m(Lcom/miui/contentcatcher/service/config/a$c;Z)Z
    :try_end_16
    .catch Ljava/lang/Exception; {:try_start_16 .. :try_end_16} :catch_13

    goto :goto_24

    :catch_13
    move-exception v0

    new-instance v3, Ljava/lang/StringBuilder;

    const-string v4, " updateStateFromDisk  e :"

    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v6, v0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    :cond_2f
    :goto_24
    sget-boolean v0, Lp/b;->a:Z

    if-eqz v0, :cond_30

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v3, "parseConfig: "

    invoke-direct {v0, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v6, v0}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    :cond_30
    iget-object v0, v1, Lcom/miui/contentcatcher/service/config/a;->c:Ljava/util/ArrayList;

    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_31
    invoke-virtual/range {p1 .. p1}, Landroid/util/JsonReader;->endObject()V

    return-void
.end method

.method public final k(Ljava/lang/String;)Lcom/miui/contentcatcher/common/ExtraConfigInfo;
    .locals 3

    new-instance v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;

    invoke-direct {v0}, Lcom/miui/contentcatcher/common/ExtraConfigInfo;-><init>()V

    const/4 v1, 0x0

    :try_start_0
    new-instance v2, Lorg/json/JSONObject;

    invoke-direct {v2, p1}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V

    const-string p1, "matchComponent"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->b:Ljava/lang/String;

    const-string p1, "matchVersionName"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->a:Ljava/lang/String;

    const-string p1, "matchId"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->c:Ljava/lang/String;

    const-string p1, "matchText"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->d:Ljava/lang/String;

    const-string p1, "matchDescText"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->e:Ljava/lang/String;

    const-string p1, "isRegexMatchText"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optBoolean(Ljava/lang/String;)Z

    move-result p1

    iput-boolean p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->f:Z

    const-string p1, "isRegexMatchDescText"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optBoolean(Ljava/lang/String;)Z

    move-result p1

    iput-boolean p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->g:Z

    const-string p1, "matchLevel"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->h:Ljava/lang/String;

    const-string p1, "matchPath"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->i:Ljava/lang/String;

    const-string p1, "matchTitlePath"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->j:Ljava/lang/String;

    const-string p1, "matchTitleId"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->k:Ljava/lang/String;

    const-string p1, "matchAuthorId"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->l:Ljava/lang/String;

    const-string p1, "matchField"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->n:Ljava/lang/String;

    const-string p1, "matchClickPath"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->o:Ljava/lang/String;

    const-string p1, "matchPriceId"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->m:Ljava/lang/String;

    const-string p1, "targetUrl"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->p:Ljava/lang/String;

    const-string p1, "targetData"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->q:Ljava/lang/String;

    const-string p1, "targetTitle"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->r:Ljava/lang/String;

    const-string p1, "targetAuthor"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->s:Ljava/lang/String;

    const-string p1, "targetImage"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->t:Ljava/lang/String;

    const-string p1, "targetPrice"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->u:Ljava/lang/String;

    const-string p1, "broadcastAction"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->v:Ljava/lang/String;

    const-string p1, "broadcastPackage"

    invoke-virtual {v2, p1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->w:Ljava/lang/String;

    invoke-virtual {p0, v0}, Lcom/miui/contentcatcher/service/config/a;->g(Lcom/miui/contentcatcher/common/ExtraConfigInfo;)Z

    move-result p1

    if-nez p1, :cond_0

    return-object v1

    :cond_0
    iget-object p1, p0, Lcom/miui/contentcatcher/service/config/a;->d:Ljava/lang/String;

    iput-object p1, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->x:Ljava/lang/String;

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/a;->e:Ljava/lang/String;

    iput-object p0, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->y:Ljava/lang/String;

    iget-object p0, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->i:Ljava/lang/String;

    invoke-virtual {p0}, Ljava/lang/String;->length()I

    move-result p0

    if-lez p0, :cond_1

    iget-object p0, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->i:Ljava/lang/String;

    invoke-static {p0}, Lp/a;->d(Ljava/lang/String;)Ljava/util/ArrayList;

    move-result-object p0

    iput-object p0, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->z:Ljava/util/ArrayList;

    goto :goto_0

    :catch_0
    move-exception p0

    goto :goto_1

    :cond_1
    :goto_0
    iget-object p0, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->o:Ljava/lang/String;

    invoke-virtual {p0}, Ljava/lang/String;->length()I

    move-result p0

    if-lez p0, :cond_2

    iget-object p0, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->o:Ljava/lang/String;

    invoke-static {p0}, Lp/a;->c(Ljava/lang/String;)Ljava/util/ArrayList;

    move-result-object p0

    iput-object p0, v0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->A:Ljava/util/ArrayList;
    :try_end_0
    .catch Lorg/json/JSONException; {:try_start_0 .. :try_end_0} :catch_0

    :cond_2
    return-object v0

    :goto_1
    new-instance p1, Ljava/lang/StringBuilder;

    const-string v0, "parseExtraFileLine: "

    invoke-direct {p1, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {p1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string p1, "ConfigHelper"

    invoke-static {p1, p0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    return-object v1
.end method
