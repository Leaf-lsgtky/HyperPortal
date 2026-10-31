.class public final Lw/a;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation build Landroid/annotation/TargetApi;
    value = 0x11
.end annotation


# static fields
.field public static volatile f:Lw/a;


# instance fields
.field public final a:Lw/b;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Lw/b<",
            "Lmiui/contentcatcher/sdk/listener/IContentListenerCallback;",
            ">;"
        }
    .end annotation
.end field

.field public final b:Lw/b;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Lw/b<",
            "Lmiui/contentcatcher/sdk/injector/IContentDecorateCallback;",
            ">;"
        }
    .end annotation
.end field

.field public final c:Ljava/util/HashMap;

.field public final d:Landroid/content/Context;

.field public final e:Lcom/miui/contentcatcher/ContentCatcherService$c;


# direct methods
.method public constructor <init>(Landroid/content/Context;Lcom/miui/contentcatcher/ContentCatcherService$c;)V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    new-instance v0, Lw/b;

    invoke-direct {v0}, Lw/b;-><init>()V

    iput-object v0, p0, Lw/a;->a:Lw/b;

    new-instance v0, Lw/b;

    invoke-direct {v0}, Lw/b;-><init>()V

    iput-object v0, p0, Lw/a;->b:Lw/b;

    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    iput-object v0, p0, Lw/a;->c:Ljava/util/HashMap;

    iput-object p1, p0, Lw/a;->d:Landroid/content/Context;

    iput-object p2, p0, Lw/a;->e:Lcom/miui/contentcatcher/ContentCatcherService$c;

    return-void
.end method


# virtual methods
.method public final a(Lmiui/contentcatcher/sdk/ClientCatcherResult;Ljava/lang/String;)Ljava/lang/String;
    .locals 2

    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/ClientCatcherResult;->getResults()Ljava/util/Map;

    move-result-object p1

    invoke-interface {p1}, Ljava/util/Map;->keySet()Ljava/util/Set;

    move-result-object p1

    invoke-interface {p1}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object p1

    :cond_0
    invoke-interface {p1}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_1

    invoke-interface {p1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v1

    if-nez v1, :cond_0

    goto :goto_0

    :cond_1
    const-string v0, ""

    :goto_0
    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result p1

    if-eqz p1, :cond_3

    iget-object p0, p0, Lw/a;->a:Lw/b;

    invoke-virtual {p0, p2}, Lw/b;->d(Ljava/lang/String;)Lmiui/contentcatcher/sdk/ClientToken;

    move-result-object p0

    if-nez p0, :cond_2

    const/4 v0, 0x0

    goto :goto_1

    :cond_2
    invoke-virtual {p0}, Lmiui/contentcatcher/sdk/ClientToken;->getJobTag()Ljava/lang/String;

    move-result-object v0

    :cond_3
    :goto_1
    return-object v0
.end method

.method public final b()V
    .locals 4

    monitor-enter p0

    :try_start_0
    iget-object v0, p0, Lw/a;->b:Lw/b;

    invoke-virtual {v0}, Lw/b;->c()Ljava/util/ArrayList;

    move-result-object v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    :try_start_1
    invoke-virtual {v0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :goto_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_0

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lmiui/contentcatcher/sdk/injector/IContentDecorateCallback;

    const/4 v2, 0x0

    invoke-interface {v1, v2}, Lmiui/contentcatcher/sdk/injector/IContentDecorateCallback;->updateContent(Lmiui/contentcatcher/sdk/DecorateContentParam;)V
    :try_end_1
    .catch Landroid/os/RemoteException; {:try_start_1 .. :try_end_1} :catch_0
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    goto :goto_0

    :catchall_0
    move-exception v0

    goto :goto_1

    :catch_0
    move-exception v0

    :try_start_2
    const-string v1, "DataHub"

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "notifyAllDecorateCallbacks failed: "

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v1, v0}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    :cond_0
    monitor-exit p0

    return-void

    :goto_1
    monitor-exit p0
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    throw v0
.end method

.method public final c(Lmiui/contentcatcher/sdk/Content;)V
    .locals 10

    monitor-enter p0

    if-nez p1, :cond_0

    :try_start_0
    monitor-exit p0

    return-void

    :catchall_0
    move-exception p1

    goto/16 :goto_3

    :cond_0
    iget v0, p1, Lmiui/contentcatcher/sdk/Content;->mCatchType:I

    const/16 v1, 0x20

    if-ne v0, v1, :cond_1

    invoke-virtual {p0, p1}, Lw/a;->d(Lmiui/contentcatcher/sdk/Content;)V

    monitor-exit p0

    return-void

    :cond_1
    const/16 v2, 0x160

    if-ne v0, v2, :cond_2

    const-string p1, "DataHub"

    const-string v0, "sendBroadcastToTaplus"

    invoke-static {p1, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p1, p0, Lw/a;->d:Landroid/content/Context;

    new-instance v0, Landroid/content/Intent;

    const-string v2, "miui.intent.action.CLIPBOARD"

    invoke-direct {v0, v2}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, v1}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;

    const-string v1, "com.miui.contentextension"

    invoke-virtual {v0, v1}, Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;

    sget-object v1, Landroid/os/UserHandle;->CURRENT:Landroid/os/UserHandle;

    invoke-virtual {p1, v0, v1}, Landroid/content/Context;->sendBroadcastAsUser(Landroid/content/Intent;Landroid/os/UserHandle;)V

    monitor-exit p0

    return-void

    :cond_2
    const/16 v1, 0x100

    if-lt v0, v1, :cond_4

    iget-object v0, p0, Lw/a;->a:Lw/b;

    invoke-virtual {v0}, Lw/b;->e()Ljava/util/ArrayList;

    move-result-object v0

    invoke-virtual {v0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :cond_3
    :goto_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_9

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    move-object v2, v1

    check-cast v2, Lmiui/contentcatcher/sdk/ClientToken;

    invoke-virtual {v2}, Lmiui/contentcatcher/sdk/ClientToken;->getParams()Ljava/util/HashMap;

    move-result-object v2

    const-string v3, "observe_control_event"

    invoke-virtual {v2, v3}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/String;
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eqz v2, :cond_3

    :try_start_1
    invoke-static {v2}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v2
    :try_end_1
    .catch Ljava/lang/NumberFormatException; {:try_start_1 .. :try_end_1} :catch_0
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    goto :goto_1

    :catch_0
    move-exception v2

    :try_start_2
    const-string v3, "DataHub"

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "onContentCatched-NumberFormatException: "

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v3, v2}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    const/4 v2, -0x1

    :goto_1
    iget v3, p1, Lmiui/contentcatcher/sdk/Content;->mCatchType:I

    if-ne v2, v3, :cond_3

    iget-object v2, p0, Lw/a;->a:Lw/b;

    invoke-virtual {v2, v1}, Lw/b;->b(Ljava/lang/Object;)Landroid/os/IInterface;

    move-result-object v1

    check-cast v1, Lmiui/contentcatcher/sdk/listener/IContentListenerCallback;

    if-eqz v1, :cond_3

    new-instance v2, Lmiui/contentcatcher/sdk/ClientCatcherResult;

    invoke-direct {v2}, Lmiui/contentcatcher/sdk/ClientCatcherResult;-><init>()V

    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/Content;->getInjectorToken()Lmiui/contentcatcher/sdk/Token;

    move-result-object v3

    invoke-virtual {v2, v3}, Lmiui/contentcatcher/sdk/ClientCatcherResult;->setInjectorToken(Lmiui/contentcatcher/sdk/Token;)V

    invoke-virtual {v2}, Lmiui/contentcatcher/sdk/ClientCatcherResult;->getPropertyMap()Ljava/util/Map;

    move-result-object v3

    const-string v4, "observe_control_event"

    iget v5, p1, Lmiui/contentcatcher/sdk/Content;->mCatchType:I

    invoke-static {v5}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;

    move-result-object v5

    invoke-interface {v3, v4, v5}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    :try_start_3
    invoke-interface {v1, v2}, Lmiui/contentcatcher/sdk/listener/IContentListenerCallback;->onContentReceived(Lmiui/contentcatcher/sdk/ClientCatcherResult;)V
    :try_end_3
    .catch Landroid/os/RemoteException; {:try_start_3 .. :try_end_3} :catch_1
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    goto :goto_0

    :catch_1
    move-exception v1

    :try_start_4
    const-string v2, "DataHub"

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "dispatch content to listeners failed: "

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v2, v1}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    goto/16 :goto_0

    :cond_4
    iget-object v0, p0, Lw/a;->d:Landroid/content/Context;

    invoke-static {v0}, Lcom/miui/contentcatcher/service/config/a;->e(Landroid/content/Context;)Lcom/miui/contentcatcher/service/config/a;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    invoke-static {p1}, Lcom/miui/contentcatcher/service/config/a;->j(Lmiui/contentcatcher/sdk/Content;)Ljava/util/HashMap;

    move-result-object v0

    if-eqz v0, :cond_9

    invoke-virtual {v0}, Ljava/util/HashMap;->isEmpty()Z

    move-result v1

    if-nez v1, :cond_9

    invoke-virtual {v0}, Ljava/util/HashMap;->entrySet()Ljava/util/Set;

    move-result-object v0

    invoke-interface {v0}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :cond_5
    :goto_2
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_9

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/util/Map$Entry;

    invoke-interface {v1}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v2

    move-object v4, v2

    check-cast v4, Ljava/lang/String;

    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lmiui/contentcatcher/sdk/ClientCatcherResult;

    iget-object v2, p0, Lw/a;->d:Landroid/content/Context;

    invoke-static {v2}, Lcom/miui/contentcatcher/service/config/a;->e(Landroid/content/Context;)Lcom/miui/contentcatcher/service/config/a;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    invoke-static {v1}, Lcom/miui/contentcatcher/service/config/a;->a(Lmiui/contentcatcher/sdk/ClientCatcherResult;)Z

    move-result v2

    if-nez v2, :cond_6

    monitor-exit p0

    return-void

    :cond_6
    iget-object v2, p0, Lw/a;->a:Lw/b;

    invoke-virtual {v2, v4}, Lw/b;->b(Ljava/lang/Object;)Landroid/os/IInterface;

    move-result-object v2

    check-cast v2, Lmiui/contentcatcher/sdk/listener/IContentListenerCallback;
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    if-eqz v2, :cond_7

    :try_start_5
    invoke-interface {v2, v1}, Lmiui/contentcatcher/sdk/listener/IContentListenerCallback;->onContentReceived(Lmiui/contentcatcher/sdk/ClientCatcherResult;)V

    invoke-virtual {p0, v1, v4}, Lw/a;->a(Lmiui/contentcatcher/sdk/ClientCatcherResult;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    const-string v7, "callback_listener"

    const-string v8, "content"

    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/Content;->getInjectorToken()Lmiui/contentcatcher/sdk/Token;

    move-result-object v9

    const/4 v6, 0x0

    move-object v3, p0

    invoke-virtual/range {v3 .. v9}, Lw/a;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lmiui/contentcatcher/sdk/Token;)V
    :try_end_5
    .catch Landroid/os/RemoteException; {:try_start_5 .. :try_end_5} :catch_2
    .catchall {:try_start_5 .. :try_end_5} :catchall_0

    goto :goto_2

    :catch_2
    move-exception v1

    :try_start_6
    const-string v2, "DataHub"

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "dispatch content to listeners failed: "

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v2, v1}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_2

    :cond_7
    iget-object v2, p0, Lw/a;->c:Ljava/util/HashMap;

    invoke-virtual {v2, v4, v1}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    invoke-static {}, Landroid/os/Message;->obtain()Landroid/os/Message;

    move-result-object v2

    const/16 v3, 0x65

    iput v3, v2, Landroid/os/Message;->what:I

    iput-object v4, v2, Landroid/os/Message;->obj:Ljava/lang/Object;

    iget-object v3, p0, Lw/a;->e:Lcom/miui/contentcatcher/ContentCatcherService$c;

    const-wide/16 v5, 0xbb8

    invoke-virtual {v3, v2, v5, v6}, Landroid/os/Handler;->sendMessageDelayed(Landroid/os/Message;J)Z

    new-instance v2, Lmiui/contentcatcher/sdk/ClientCatcherResult;

    invoke-direct {v2}, Lmiui/contentcatcher/sdk/ClientCatcherResult;-><init>()V

    invoke-virtual {v1}, Lmiui/contentcatcher/sdk/ClientCatcherResult;->getInjectorToken()Lmiui/contentcatcher/sdk/Token;

    move-result-object v1

    invoke-virtual {v2, v1}, Lmiui/contentcatcher/sdk/ClientCatcherResult;->setInjectorToken(Lmiui/contentcatcher/sdk/Token;)V

    iget-object v1, p0, Lw/a;->d:Landroid/content/Context;

    invoke-static {v1}, LA/a;->a(Landroid/content/Context;)Z

    move-result v1

    iget-object v3, p0, Lw/a;->d:Landroid/content/Context;

    invoke-static {v3}, Lcom/miui/contentcatcher/service/config/a;->e(Landroid/content/Context;)Lcom/miui/contentcatcher/service/config/a;

    move-result-object v3

    invoke-virtual {v3, v4, v2, v1}, Lcom/miui/contentcatcher/service/config/a;->b(Ljava/lang/String;Lmiui/contentcatcher/sdk/ClientCatcherResult;Z)Landroid/content/Intent;

    move-result-object v2
    :try_end_6
    .catchall {:try_start_6 .. :try_end_6} :catchall_0

    if-eqz v2, :cond_5

    if-eqz v1, :cond_8

    :try_start_7
    iget-object v1, p0, Lw/a;->d:Landroid/content/Context;

    const-string v3, "startServiceAsUser"

    const-class v4, Landroid/content/Intent;

    const-class v5, Landroid/os/UserHandle;

    filled-new-array {v4, v5}, [Ljava/lang/Class;

    move-result-object v4

    sget-object v5, Landroid/os/UserHandle;->CURRENT:Landroid/os/UserHandle;

    filled-new-array {v2, v5}, [Ljava/lang/Object;

    move-result-object v2

    invoke-static {v1, v3, v4, v2}, Lp/c;->b(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_7
    .catch Ljava/lang/Exception; {:try_start_7 .. :try_end_7} :catch_3
    .catchall {:try_start_7 .. :try_end_7} :catchall_0

    goto/16 :goto_2

    :catch_3
    move-exception v1

    :try_start_8
    const-string v2, "DataHub"

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "startServiceAsUser failed: "

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v2, v1}, Landroid/util/Slog;->e(Ljava/lang/String;Ljava/lang/String;)I

    goto/16 :goto_2

    :cond_8
    iget-object v1, p0, Lw/a;->d:Landroid/content/Context;

    sget-object v3, Landroid/os/UserHandle;->CURRENT:Landroid/os/UserHandle;

    invoke-virtual {v1, v2, v3}, Landroid/content/Context;->sendBroadcastAsUser(Landroid/content/Intent;Landroid/os/UserHandle;)V

    goto/16 :goto_2

    :cond_9
    monitor-exit p0

    return-void

    :goto_3
    monitor-exit p0
    :try_end_8
    .catchall {:try_start_8 .. :try_end_8} :catchall_0

    throw p1
.end method

.method public final d(Lmiui/contentcatcher/sdk/Content;)V
    .locals 12

    const-string v0, "dispatch event to listeners failed: "

    iget-object v1, p1, Lmiui/contentcatcher/sdk/Content;->mResultMap:Ljava/util/Map;

    if-nez v1, :cond_0

    return-void

    :cond_0
    const-string v2, "MotionEvent"

    invoke-interface {v1, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    instance-of v3, v1, Landroid/view/MotionEvent;

    if-eqz v3, :cond_3

    new-instance v3, Lmiui/contentcatcher/sdk/ClientCatcherResult;

    invoke-direct {v3}, Lmiui/contentcatcher/sdk/ClientCatcherResult;-><init>()V

    invoke-virtual {v3, v2, v1}, Lmiui/contentcatcher/sdk/ClientCatcherResult;->addProperty(Ljava/lang/String;Ljava/lang/Object;)V

    iget-object v2, p0, Lw/a;->a:Lw/b;

    const-string v4, "com.miui.voiceassistant"

    invoke-virtual {v2, v4}, Lw/b;->b(Ljava/lang/Object;)Landroid/os/IInterface;

    move-result-object v5

    check-cast v5, Lmiui/contentcatcher/sdk/listener/IContentListenerCallback;

    if-eqz v5, :cond_3

    :try_start_0
    invoke-interface {v5, v3}, Lmiui/contentcatcher/sdk/listener/IContentListenerCallback;->onContentReceived(Lmiui/contentcatcher/sdk/ClientCatcherResult;)V

    invoke-virtual {v2, v4}, Lw/b;->d(Ljava/lang/String;)Lmiui/contentcatcher/sdk/ClientToken;

    move-result-object v2

    const/4 v3, 0x0

    if-nez v2, :cond_1

    move-object v6, v3

    goto :goto_0

    :cond_1
    invoke-virtual {v2}, Lmiui/contentcatcher/sdk/ClientToken;->getPkgName()Ljava/lang/String;

    move-result-object v4

    move-object v6, v4

    :goto_0
    if-nez v2, :cond_2

    :goto_1
    move-object v7, v3

    goto :goto_2

    :cond_2
    invoke-virtual {v2}, Lmiui/contentcatcher/sdk/ClientToken;->getJobTag()Ljava/lang/String;

    move-result-object v3

    goto :goto_1

    :goto_2
    const-string v9, "callback_listener"

    const-string v10, "motion_event"

    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/Content;->getInjectorToken()Lmiui/contentcatcher/sdk/Token;

    move-result-object v11

    const/4 v8, 0x0

    move-object v5, p0

    invoke-virtual/range {v5 .. v11}, Lw/a;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lmiui/contentcatcher/sdk/Token;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    :goto_3
    check-cast v1, Landroid/view/MotionEvent;

    invoke-virtual {v1}, Landroid/view/MotionEvent;->recycle()V

    goto :goto_5

    :catchall_0
    move-exception p0

    goto :goto_4

    :catch_0
    move-exception p0

    :try_start_1
    const-string p1, "DataHub"

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v2, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {p1, p0}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    goto :goto_3

    :goto_4
    check-cast v1, Landroid/view/MotionEvent;

    invoke-virtual {v1}, Landroid/view/MotionEvent;->recycle()V

    throw p0

    :cond_3
    :goto_5
    return-void
.end method

.method public final e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lmiui/contentcatcher/sdk/Token;)V
    .locals 7

    const-string v0, "DataHub"

    const-string v1, "reportMiuiContentCatcher, jsonObject: "

    const-string v2, "no_job_for_"

    new-instance v3, Lorg/json/JSONObject;

    invoke-direct {v3}, Lorg/json/JSONObject;-><init>()V

    :try_start_0
    invoke-static {p1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v4
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    const-string v5, "other_target"

    if-eqz v4, :cond_0

    move-object p1, v5

    :cond_0
    :try_start_1
    invoke-static {p2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v4
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_0

    const-string v6, "other_job"

    if-eqz v4, :cond_1

    move-object p2, v6

    :cond_1
    :try_start_2
    invoke-static {p2, v6}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z

    move-result v4

    if-eqz v4, :cond_2

    invoke-static {p1, v5}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z

    move-result v4

    if-nez v4, :cond_2

    new-instance p2, Ljava/lang/StringBuilder;

    invoke-direct {p2, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    goto :goto_0

    :catch_0
    move-exception p0

    goto/16 :goto_3

    :cond_2
    :goto_0
    const-string v2, "target"

    invoke-virtual {v3, v2, p1}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string p1, "job"

    invoke-virtual {v3, p1, p2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string p1, "enable"

    invoke-virtual {v3, p1, p3}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string p1, "upload_way"

    invoke-virtual {v3, p1, p4}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string p1, "data_type"

    invoke-virtual {v3, p1, p5}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string p1, "app"

    const/4 p2, 0x0

    if-eqz p6, :cond_3

    invoke-virtual {p6}, Lmiui/contentcatcher/sdk/Token;->getPkgName()Ljava/lang/String;

    move-result-object p3

    goto :goto_1

    :cond_3
    move-object p3, p2

    :goto_1
    invoke-virtual {v3, p1, p3}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string p1, "activity"

    if-eqz p6, :cond_4

    invoke-virtual {p6}, Lmiui/contentcatcher/sdk/Token;->getActivityName()Ljava/lang/String;

    move-result-object p2

    :cond_4
    invoke-virtual {v3, p1, p2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    new-instance p1, Lorg/json/JSONObject;

    invoke-direct {p1}, Lorg/json/JSONObject;-><init>()V

    const-string p2, "ext"

    invoke-virtual {p1}, Lorg/json/JSONObject;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v3, p2, p1}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3}, Lorg/json/JSONObject;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v0, p1}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p1, Landroid/content/Intent;

    const-string p2, "onetrack.action.TRACK_EVENT"

    invoke-direct {p1, p2}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V

    const-string p2, "com.miui.analytics"

    invoke-virtual {p1, p2}, Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;

    const-string p2, "APP_ID"

    const-string p3, "31000000209"

    invoke-virtual {p1, p2, p3}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    const-string p2, "PACKAGE"

    const-string p3, "com.miui.contentcatcher"

    invoke-virtual {p1, p2, p3}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    const-string p2, "EVENT_NAME"

    const-string p3, "send_message"

    invoke-virtual {p1, p2, p3}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    invoke-virtual {v3}, Lorg/json/JSONObject;->keys()Ljava/util/Iterator;

    move-result-object p2

    :goto_2
    invoke-interface {p2}, Ljava/util/Iterator;->hasNext()Z

    move-result p3

    if-eqz p3, :cond_5

    invoke-interface {p2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object p3

    check-cast p3, Ljava/lang/String;

    invoke-virtual {v3, p3}, Lorg/json/JSONObject;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p4

    invoke-virtual {p1, p3, p4}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    goto :goto_2

    :cond_5
    const/4 p2, 0x1

    invoke-virtual {p1, p2}, Landroid/content/Intent;->setFlags(I)Landroid/content/Intent;

    iget-object p0, p0, Lw/a;->d:Landroid/content/Context;

    invoke-virtual {p0, p1}, Landroid/content/Context;->startService(Landroid/content/Intent;)Landroid/content/ComponentName;
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_0

    goto :goto_4

    :goto_3
    new-instance p1, Ljava/lang/StringBuilder;

    const-string p2, "reportMiuiContentCatcher, e: "

    invoke-direct {p1, p2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v0, p0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    :goto_4
    return-void
.end method

.method public final f(Ljava/lang/String;)V
    .locals 9

    iget-object v0, p0, Lw/a;->c:Ljava/util/HashMap;

    invoke-virtual {v0, p1}, Ljava/util/HashMap;->remove(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lmiui/contentcatcher/sdk/ClientCatcherResult;

    if-eqz v0, :cond_0

    iget-object v1, p0, Lw/a;->a:Lw/b;

    invoke-virtual {v1, p1}, Lw/b;->b(Ljava/lang/Object;)Landroid/os/IInterface;

    move-result-object v1

    check-cast v1, Lmiui/contentcatcher/sdk/listener/IContentListenerCallback;

    if-eqz v1, :cond_0

    :try_start_0
    invoke-interface {v1, v0}, Lmiui/contentcatcher/sdk/listener/IContentListenerCallback;->onContentReceived(Lmiui/contentcatcher/sdk/ClientCatcherResult;)V

    invoke-virtual {p0, v0, p1}, Lw/a;->a(Lmiui/contentcatcher/sdk/ClientCatcherResult;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v4

    const-string v6, "callback_listener"

    const-string v7, "content_delay"

    invoke-virtual {v0}, Lmiui/contentcatcher/sdk/ClientCatcherResult;->getInjectorToken()Lmiui/contentcatcher/sdk/Token;

    move-result-object v8

    const/4 v5, 0x0

    move-object v2, p0

    move-object v3, p1

    invoke-virtual/range {v2 .. v8}, Lw/a;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lmiui/contentcatcher/sdk/Token;)V
    :try_end_0
    .catch Landroid/os/RemoteException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p0

    new-instance p1, Ljava/lang/StringBuilder;

    const-string v0, "sendPendingResult-onContentReceived failed: "

    invoke-direct {p1, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {p1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string p1, "DataHub"

    invoke-static {p1, p0}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    :cond_0
    :goto_0
    return-void
.end method
