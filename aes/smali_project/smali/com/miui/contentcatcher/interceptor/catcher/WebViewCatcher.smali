.class public Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;
.super Lr/b;
.source "SourceFile"

# interfaces
.implements Lmiui/contentcatcher/sdk/WebViewDetector$Callback;


# instance fields
.field public final l:Ljava/util/WeakHashMap;

.field public final m:Ljava/util/HashMap;

.field public final n:I


# direct methods
.method public constructor <init>(Landroid/app/Activity;)V
    .locals 1

    invoke-direct {p0, p1}, Lr/b;-><init>(Landroid/app/Activity;)V

    new-instance v0, Ljava/util/WeakHashMap;

    invoke-direct {v0}, Ljava/util/WeakHashMap;-><init>()V

    iput-object v0, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->l:Ljava/util/WeakHashMap;

    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    iput-object v0, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->m:Ljava/util/HashMap;

    invoke-static {p0}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I

    move-result v0

    invoke-static {v0}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    invoke-static {p1}, Landroid/view/ViewConfiguration;->get(Landroid/content/Context;)Landroid/view/ViewConfiguration;

    invoke-static {}, Landroid/view/ViewConfiguration;->getLongPressTimeout()I

    move-result p1

    iput p1, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->n:I

    return-void
.end method


# virtual methods
.method public final c()I
    .locals 0

    const/4 p0, 0x0

    return p0
.end method

.method public final d()I
    .locals 0

    const/4 p0, 0x1

    return p0
.end method

.method public final e()Ljava/lang/String;
    .locals 0

    const-string p0, "WebViewCatcher"

    return-object p0
.end method

.method public final g()Z
    .locals 0

    const/4 p0, 0x0

    return p0
.end method

.method public final h(Landroid/view/View;Z)V
    .locals 2

    iget-object v0, p0, Lr/b;->d:Lcom/miui/contentcatcher/Interceptor;

    invoke-virtual {v0}, Lcom/miui/contentcatcher/Interceptor;->h()Landroid/os/Handler;

    move-result-object v0

    if-eqz v0, :cond_0

    new-instance v1, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$b;

    invoke-direct {v1, p0, p2, p1}, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$b;-><init>(Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;ZLandroid/view/View;)V

    invoke-virtual {v0, v1}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    :cond_0
    return-void
.end method

.method public final i(Ljava/util/Map;)V
    .locals 5
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/Object;",
            ">;)V"
        }
    .end annotation

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    invoke-interface {p1}, Ljava/util/Map;->keySet()Ljava/util/Set;

    move-result-object v1

    invoke-interface {v1}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :cond_0
    :goto_0
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_1

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/String;

    if-eqz v2, :cond_0

    new-instance v3, Ljava/lang/StringBuilder;

    const-string v4, "paramKey: "

    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v4, ", value: "

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-interface {p1, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    const-string v4, "WebViewCatcher"

    invoke-static {v4, v3}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    const-string v3, "#urls"

    invoke-virtual {v2, v3}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v3

    if-eqz v3, :cond_0

    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    const-string v3, "#"

    invoke-virtual {v2, v3}, Ljava/lang/String;->indexOf(Ljava/lang/String;)I

    move-result v3

    const/4 v4, 0x0

    invoke-virtual {v2, v4, v3}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v3

    invoke-interface {p1, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    if-eqz v2, :cond_0

    iget-object v4, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->m:Ljava/util/HashMap;

    check-cast v2, Ljava/lang/String;

    invoke-virtual {v4, v3, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    goto :goto_0

    :cond_1
    invoke-virtual {v0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :goto_1
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_2

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    invoke-interface {p1, v0}, Ljava/util/Map;->remove(Ljava/lang/Object;)Ljava/lang/Object;

    goto :goto_1

    :cond_2
    return-void
.end method

.method public final j()V
    .locals 2

    invoke-static {}, Lmiui/contentcatcher/sdk/WebViewDetector;->getInstance()Lmiui/contentcatcher/sdk/WebViewDetector;

    move-result-object p0

    sget-object v0, Ls/c;->a:Ls/c;

    if-nez v0, :cond_1

    const-class v0, Ls/c;

    monitor-enter v0

    :try_start_0
    sget-object v1, Ls/c;->a:Ls/c;

    if-nez v1, :cond_0

    new-instance v1, Ls/c;

    invoke-direct {v1}, Ls/c;-><init>()V

    sput-object v1, Ls/c;->a:Ls/c;

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
    sget-object v0, Ls/c;->a:Ls/c;

    invoke-virtual {p0, v0}, Lmiui/contentcatcher/sdk/WebViewDetector;->setCustomDetector(Lmiui/contentcatcher/sdk/WebViewDetector;)V

    return-void
.end method

.method public final k()V
    .locals 0

    invoke-super {p0}, Lr/b;->k()V

    iget-object p0, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->l:Ljava/util/WeakHashMap;

    invoke-virtual {p0}, Ljava/util/WeakHashMap;->clear()V

    return-void
.end method

.method public final l()V
    .locals 3

    const/4 v0, 0x0

    iput-boolean v0, p0, Lr/b;->e:Z

    invoke-static {}, Lmiui/contentcatcher/sdk/WebViewDetector;->getInstance()Lmiui/contentcatcher/sdk/WebViewDetector;

    move-result-object v0

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Lmiui/contentcatcher/sdk/WebViewDetector;->setActiveCallback(Lmiui/contentcatcher/sdk/WebViewDetector$Callback;)V

    iget-object p0, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->l:Ljava/util/WeakHashMap;

    invoke-virtual {p0}, Ljava/util/WeakHashMap;->values()Ljava/util/Collection;

    move-result-object p0

    invoke-interface {p0}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :cond_0
    :goto_0
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_1

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ls/e;

    if-eqz v0, :cond_0

    iget-object v2, v0, Ls/e;->d:Ls/e$b;

    invoke-virtual {v2, v1}, Landroid/os/Handler;->removeCallbacksAndMessages(Ljava/lang/Object;)V

    const/16 v2, 0xb

    iput v2, v0, Ls/e;->g:I

    goto :goto_0

    :cond_1
    return-void
.end method

.method public final m()V
    .locals 5

    const/4 v0, 0x1

    iput-boolean v0, p0, Lr/b;->e:Z

    invoke-static {}, Lmiui/contentcatcher/sdk/WebViewDetector;->getInstance()Lmiui/contentcatcher/sdk/WebViewDetector;

    move-result-object v0

    invoke-virtual {v0, p0}, Lmiui/contentcatcher/sdk/WebViewDetector;->setActiveCallback(Lmiui/contentcatcher/sdk/WebViewDetector$Callback;)V

    iget-object v0, p0, Lr/b;->k:Ljava/lang/Object;

    monitor-enter v0

    :try_start_0
    iget-object v1, p0, Lr/b;->d:Lcom/miui/contentcatcher/Interceptor;

    if-eqz v1, :cond_0

    iget-object v1, v1, Lcom/miui/contentcatcher/Interceptor;->o:Ljava/util/LinkedList;

    invoke-static {v1}, Lv/c;->a(Ljava/util/LinkedList;)Ljava/util/HashSet;

    move-result-object v1

    invoke-virtual {v1}, Ljava/util/HashSet;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :goto_0
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_1

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Landroid/view/View;

    iget-object v3, p0, Lr/b;->d:Lcom/miui/contentcatcher/Interceptor;

    invoke-virtual {v3}, Lcom/miui/contentcatcher/Interceptor;->h()Landroid/os/Handler;

    move-result-object v3

    new-instance v4, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$a;

    invoke-direct {v4, p0, v2}, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$a;-><init>(Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;Landroid/view/View;)V

    invoke-virtual {v3, v4}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    goto :goto_0

    :catchall_0
    move-exception p0

    goto :goto_1

    :cond_0
    const-string p0, "WebViewCatcher"

    const-string v1, "mInfoProvider is NULL"

    invoke-static {p0, v1}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    :cond_1
    monitor-exit v0

    return-void

    :goto_1
    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw p0
.end method

.method public final n()V
    .locals 0

    invoke-super {p0}, Lr/b;->n()V

    iget-object p0, p0, Lr/b;->a:Ljava/util/LinkedHashMap;

    invoke-virtual {p0}, Ljava/util/LinkedHashMap;->clear()V

    return-void
.end method

.method public final o(Landroid/view/View;)V
    .locals 14

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "initiateWebViewOperate webView.hashCode(): "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1}, Ljava/lang/Object;->hashCode()I

    move-result v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "WebViewCatcher"

    invoke-static {v1, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {p1}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I

    move-result v0

    invoke-static {v0}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v0

    iget-object v2, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->l:Ljava/util/WeakHashMap;

    invoke-virtual {v2, v0}, Ljava/util/WeakHashMap;->containsKey(Ljava/lang/Object;)Z

    move-result v2

    const/4 v3, 0x0

    const-string v4, "WebViewController"

    const/4 v5, 0x1

    if-nez v2, :cond_6

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p1}, Ljava/lang/Object;->hashCode()I

    move-result v6

    invoke-virtual {v2, v6}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v6, " not found in mWebViewMap"

    invoke-virtual {v2, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v2, Ljava/util/ArrayList;

    invoke-direct {v2}, Ljava/util/ArrayList;-><init>()V

    new-instance v6, Ljava/util/ArrayList;

    invoke-direct {v6}, Ljava/util/ArrayList;-><init>()V

    iget-object v7, p0, Lr/b;->b:Ljava/util/ArrayList;

    invoke-virtual {v7}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v7

    :cond_0
    :goto_0
    invoke-interface {v7}, Ljava/util/Iterator;->hasNext()Z

    move-result v8

    const/4 v9, 0x4

    if-eqz v8, :cond_2

    invoke-interface {v7}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v8

    check-cast v8, Lt/a;

    invoke-virtual {v8, p1, v5}, Lt/a;->a(Landroid/view/View;I)Z

    move-result v10

    if-eqz v10, :cond_1

    iget-object v10, v8, Lt/a;->b:Ljava/lang/String;

    invoke-virtual {v2, v10}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_1
    invoke-virtual {v8, p1, v9}, Lt/a;->a(Landroid/view/View;I)Z

    move-result v9

    if-eqz v9, :cond_0

    iget-object v8, v8, Lt/a;->b:Ljava/lang/String;

    invoke-virtual {v6, v8}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_0

    :cond_2
    invoke-virtual {v2}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v7

    if-eqz v7, :cond_3

    invoke-virtual {v6}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v7

    if-eqz v7, :cond_3

    const-string p1, "Both lists are empty"

    invoke-static {v1, p1}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p1, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->l:Ljava/util/WeakHashMap;

    invoke-virtual {p1, v0, v3}, Ljava/util/WeakHashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    move-object v8, v3

    goto/16 :goto_2

    :cond_3
    iget-object v7, p0, Lr/b;->c:Lcom/miui/contentcatcher/Interceptor;

    new-instance v8, Ls/e;

    invoke-direct {v8}, Ljava/lang/Object;-><init>()V

    sget-object v10, Ls/e$a;->a:Ls/e$a;

    iput-object v10, v8, Ls/e;->a:Ls/e$a;

    iput-object v3, v8, Ls/e;->c:Ls/h;

    iput-object v3, v8, Ls/e;->e:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;

    iput-object v3, v8, Ls/e;->f:Lcom/miui/contentcatcher/Interceptor;

    const/4 v10, 0x0

    iput v10, v8, Ls/e;->g:I

    new-instance v11, Ljava/util/ArrayList;

    invoke-direct {v11}, Ljava/util/ArrayList;-><init>()V

    iput-object v11, v8, Ls/e;->h:Ljava/util/ArrayList;

    new-instance v12, Ljava/util/ArrayList;

    invoke-direct {v12}, Ljava/util/ArrayList;-><init>()V

    iput-object v12, v8, Ls/e;->i:Ljava/util/ArrayList;

    iput-object v3, v8, Ls/e;->k:Ljava/lang/String;

    iput-object p0, v8, Ls/e;->e:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;

    iput-object v7, v8, Ls/e;->f:Lcom/miui/contentcatcher/Interceptor;

    new-instance v7, Ls/e$b;

    invoke-direct {v7}, Landroid/os/Handler;-><init>()V

    new-instance v13, Ljava/lang/ref/WeakReference;

    invoke-direct {v13, v8}, Ljava/lang/ref/WeakReference;-><init>(Ljava/lang/Object;)V

    iput-object v13, v7, Ls/e$b;->a:Ljava/lang/ref/WeakReference;

    iput-object v7, v8, Ls/e;->d:Ls/e$b;

    iput-object v0, v8, Ls/e;->b:Ljava/lang/String;

    invoke-static {p1}, Ld/a;->c(Landroid/view/View;)Ls/h;

    move-result-object p1

    iput-object p1, v8, Ls/e;->c:Ls/h;

    if-eqz p1, :cond_4

    iput-object v8, p1, Ls/h;->a:Ls/e;

    invoke-virtual {p1}, Ls/h;->g()V

    :cond_4
    iput v10, v8, Ls/e;->g:I

    invoke-virtual {p0, v9}, Lr/b;->f(I)Z

    move-result p1

    iput-boolean p1, v8, Ls/e;->l:Z

    iget-object p1, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->m:Ljava/util/HashMap;

    invoke-static {v2, p1, v11}, Ls/e;->f(Ljava/util/ArrayList;Ljava/util/HashMap;Ljava/util/ArrayList;)V

    invoke-static {v6, v3, v12}, Ls/e;->f(Ljava/util/ArrayList;Ljava/util/HashMap;Ljava/util/ArrayList;)V

    iget-object p1, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->l:Ljava/util/WeakHashMap;

    invoke-virtual {p1, v0, v8}, Ljava/util/WeakHashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sput-boolean v10, Ls/e;->m:Z

    const-string p1, "startMonitorLoadingContent sJsInject false"

    invoke-static {v4, p1}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p1, v8, Ls/e;->c:Ls/h;

    if-nez p1, :cond_5

    move-object p1, v3

    goto :goto_1

    :cond_5
    invoke-virtual {p1}, Ls/h;->f()Landroid/view/View;

    move-result-object p1

    :goto_1
    if-eqz p1, :cond_7

    sget-object p1, Ls/e$a;->b:Ls/e$a;

    iput-object p1, v8, Ls/e;->a:Ls/e$a;

    iput v10, v8, Ls/e;->g:I

    const/4 p1, 0x2

    invoke-virtual {v7, p1}, Landroid/os/Handler;->removeMessages(I)V

    const-wide/16 v9, 0x7d0

    invoke-virtual {v7, p1, v9, v10}, Landroid/os/Handler;->sendEmptyMessageDelayed(IJ)Z

    goto :goto_2

    :cond_6
    iget-object p1, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->l:Ljava/util/WeakHashMap;

    invoke-virtual {p1, v0}, Ljava/util/WeakHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    move-object v8, p1

    check-cast v8, Ls/e;

    :cond_7
    :goto_2
    if-eqz v8, :cond_b

    invoke-virtual {p0, v5}, Lr/b;->f(I)Z

    move-result p0

    if-eqz p0, :cond_b

    const-string p0, "includeCatchType-true"

    invoke-static {v1, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p0, v8, Ls/e;->c:Ls/h;

    if-nez p0, :cond_8

    goto :goto_3

    :cond_8
    invoke-virtual {p0}, Ls/h;->f()Landroid/view/View;

    move-result-object v3

    :goto_3
    if-eqz v3, :cond_b

    new-instance p0, Ljava/lang/StringBuilder;

    const-string p1, "requestHtmlBodyDelay-mContentLoadingState = "

    invoke-direct {p0, p1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object p1, v8, Ls/e;->a:Ls/e$a;

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v4, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p0, v8, Ls/e;->e:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;

    iget-boolean p0, p0, Lr/b;->e:Z

    if-nez p0, :cond_9

    const-string p0, "ignore, mIsWorking==false"

    invoke-static {v4, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_4

    :cond_9
    iget-object p0, v8, Ls/e;->a:Ls/e$a;

    sget-object p1, Ls/e$a;->c:Ls/e$a;

    if-eq p0, p1, :cond_a

    const-string p0, "ignore, content loading not finished"

    invoke-static {v4, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_4

    :cond_a
    iget-object p0, v8, Ls/e;->d:Ls/e$b;

    invoke-virtual {p0, v5}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    :cond_b
    :goto_4
    return-void
.end method

.method public final onWebContentCatched(Ljava/lang/String;)V
    .locals 12

    const-string v0, "@@"

    const-string v1, "WebViewCatcher"

    const-string v2, "onWebContentCatched: "

    :try_start_0
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v2, p0, Lr/b;->c:Lcom/miui/contentcatcher/Interceptor;

    if-nez v2, :cond_0

    return-void

    :cond_0
    iget-object v2, p0, Lr/b;->d:Lcom/miui/contentcatcher/Interceptor;

    if-eqz v2, :cond_2

    iget-object v2, v2, Lcom/miui/contentcatcher/Interceptor;->d:Lu/a;

    if-eqz v2, :cond_1

    iget-boolean v2, v2, Lu/a;->w:Z

    if-eqz v2, :cond_1

    goto :goto_0

    :cond_1
    return-void

    :cond_2
    :goto_0
    if-eqz p1, :cond_b

    invoke-virtual {p1, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v2

    if-nez v2, :cond_3

    goto/16 :goto_4

    :cond_3
    invoke-virtual {p1, v0}, Ljava/lang/String;->indexOf(Ljava/lang/String;)I

    move-result v0

    const/4 v2, 0x0

    invoke-virtual {p1, v2, v0}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v3

    const/4 v4, 0x2

    add-int/2addr v0, v4

    invoke-virtual {p1, v0}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object p1

    iget-object v0, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->l:Ljava/util/WeakHashMap;

    invoke-virtual {v0, v3}, Ljava/util/WeakHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ls/e;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_3
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_1

    if-eqz v0, :cond_c

    iget-object v3, p0, Lr/b;->a:Ljava/util/LinkedHashMap;

    :try_start_1
    invoke-virtual {v3}, Ljava/util/LinkedHashMap;->clear()V

    iget-object v5, v0, Ls/e;->i:Ljava/util/ArrayList;

    invoke-virtual {v5}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v5

    :catch_0
    :cond_4
    :goto_1
    invoke-interface {v5}, Ljava/util/Iterator;->hasNext()Z

    move-result v6

    const/4 v7, 0x4

    if-eqz v6, :cond_a

    invoke-interface {v5}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ls/f;
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_3
    .catch Ljava/lang/Error; {:try_start_1 .. :try_end_1} :catch_1

    :try_start_2
    iget-object v8, v0, Ls/e;->j:Ljava/lang/String;

    invoke-virtual {v6, v8}, Ls/f;->a(Ljava/lang/String;)Z

    move-result v8
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_0
    .catch Ljava/lang/Error; {:try_start_2 .. :try_end_2} :catch_1

    iget-object v6, v6, Ls/f;->a:Ljava/lang/String;

    if-nez v8, :cond_5

    goto :goto_1

    :cond_5
    :try_start_3
    const-string v8, "image_pick_mode"

    invoke-virtual {v6, v8}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v8

    const/4 v9, 0x1

    if-eqz v8, :cond_9

    const-string v8, "img:"

    invoke-virtual {p1, v8}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v8

    if-eqz v8, :cond_4

    invoke-virtual {p1, v7}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object p1

    const-string v7, "content://"

    invoke-virtual {p1, v7}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v7
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_0
    .catch Ljava/lang/Error; {:try_start_3 .. :try_end_3} :catch_1

    iget-object v8, p0, Lr/b;->h:Landroid/app/Activity;

    const-string v10, "viewBitmap"

    const/4 v11, 0x0

    if-eqz v7, :cond_6

    :try_start_4
    invoke-virtual {v8}, Landroid/app/Activity;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v7

    invoke-static {p1}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;

    move-result-object v8

    invoke-static {v7, v8}, Landroid/provider/MediaStore$Images$Media;->getBitmap(Landroid/content/ContentResolver;Landroid/net/Uri;)Landroid/graphics/Bitmap;

    move-result-object v7

    if-eqz v7, :cond_7

    new-instance v11, Landroid/os/Bundle;

    invoke-direct {v11}, Landroid/os/Bundle;-><init>()V

    invoke-virtual {v11, v10, v7}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    goto :goto_2

    :catch_1
    move-exception p0

    goto/16 :goto_5

    :cond_6
    const-string v7, "http"

    invoke-virtual {p1, v7}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v7

    if-nez v7, :cond_7

    const-string v7, "//"

    invoke-virtual {p1, v7}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v7

    if-nez v7, :cond_7

    invoke-virtual {v8}, Landroid/app/Activity;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v7

    invoke-static {p1}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;

    move-result-object v8

    invoke-static {v7, v8}, Landroid/provider/MediaStore$Images$Media;->getBitmap(Landroid/content/ContentResolver;Landroid/net/Uri;)Landroid/graphics/Bitmap;

    move-result-object v7

    if-eqz v7, :cond_7

    new-instance v11, Landroid/os/Bundle;

    invoke-direct {v11}, Landroid/os/Bundle;-><init>()V

    invoke-virtual {v11, v10, v7}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    :cond_7
    :goto_2
    if-nez v11, :cond_8

    new-instance v11, Landroid/os/Bundle;

    invoke-direct {v11}, Landroid/os/Bundle;-><init>()V

    const-string v7, "imgUrl"

    invoke-virtual {v11, v7, p1}, Landroid/os/Bundle;->putString(Ljava/lang/String;Ljava/lang/String;)V

    :cond_8
    const-string v7, "catchType"

    invoke-virtual {v11, v7, v4}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    invoke-interface {v3, v6, v11}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_4
    .catch Ljava/lang/Exception; {:try_start_4 .. :try_end_4} :catch_0
    .catch Ljava/lang/Error; {:try_start_4 .. :try_end_4} :catch_1

    :try_start_5
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v6, "onWebContentCatched imgBundle: "

    invoke-virtual {v2, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_5
    .catch Ljava/lang/Exception; {:try_start_5 .. :try_end_5} :catch_2
    .catch Ljava/lang/Error; {:try_start_5 .. :try_end_5} :catch_1

    :catch_2
    :goto_3
    move v2, v9

    goto/16 :goto_1

    :cond_9
    :try_start_6
    const-string v8, "pick_mode"

    invoke-virtual {v6, v8}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v8

    if-eqz v8, :cond_4

    const-string v8, "txt:"

    invoke-virtual {p1, v8}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v8

    if-eqz v8, :cond_4

    invoke-virtual {p1, v7}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object p1

    if-eqz p1, :cond_4

    invoke-virtual {p1}, Ljava/lang/String;->length()I

    move-result v7

    if-lez v7, :cond_4

    invoke-interface {v3, v6, p1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_6
    .catch Ljava/lang/Exception; {:try_start_6 .. :try_end_6} :catch_0
    .catch Ljava/lang/Error; {:try_start_6 .. :try_end_6} :catch_1

    goto :goto_3

    :catch_3
    move-exception p0

    goto :goto_6

    :cond_a
    if-eqz v2, :cond_c

    :try_start_7
    iget-object p0, p0, Lr/b;->c:Lcom/miui/contentcatcher/Interceptor;

    invoke-virtual {p0, v7, v3}, Lcom/miui/contentcatcher/Interceptor;->k(ILjava/util/LinkedHashMap;)V
    :try_end_7
    .catch Ljava/lang/Exception; {:try_start_7 .. :try_end_7} :catch_3
    .catch Ljava/lang/Error; {:try_start_7 .. :try_end_7} :catch_1

    goto :goto_7

    :cond_b
    :goto_4
    return-void

    :goto_5
    const-string p1, "onWebContentCatched-Error: "

    invoke-static {p1, p0, v1}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_7

    :goto_6
    const-string p1, "onWebContentCatched-Exception: "

    invoke-static {p1, p0, v1}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_c
    :goto_7
    return-void
.end method
