.class public final Ls/e;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Ls/e$a;,
        Ls/e$b;
    }
.end annotation


# static fields
.field public static volatile m:Z = false


# instance fields
.field public a:Ls/e$a;

.field public b:Ljava/lang/String;

.field public c:Ls/h;

.field public d:Ls/e$b;

.field public e:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;

.field public f:Lcom/miui/contentcatcher/Interceptor;

.field public g:I

.field public h:Ljava/util/ArrayList;

.field public i:Ljava/util/ArrayList;

.field public j:Ljava/lang/String;

.field public k:Ljava/lang/String;

.field public l:Z


# direct methods
.method public static f(Ljava/util/ArrayList;Ljava/util/HashMap;Ljava/util/ArrayList;)V
    .locals 5

    invoke-virtual {p0}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v0

    if-eqz v0, :cond_0

    return-void

    :cond_0
    invoke-virtual {p0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :goto_0
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_4

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    const/4 v1, 0x0

    if-eqz p1, :cond_1

    invoke-virtual {p1, v0}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_1

    invoke-virtual {p1, v0}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/String;

    goto :goto_1

    :cond_1
    move-object v2, v1

    :goto_1
    new-instance v3, Ls/f;

    invoke-direct {v3}, Ljava/lang/Object;-><init>()V

    iput-object v1, v3, Ls/f;->c:Landroid/util/LruCache;

    iput-object v0, v3, Ls/f;->a:Ljava/lang/String;

    iput-object v2, v3, Ls/f;->b:Ljava/lang/String;

    if-nez v2, :cond_2

    iput-object v1, v3, Ls/f;->c:Landroid/util/LruCache;

    goto :goto_2

    :cond_2
    sget-object v0, Ls/f;->d:Ljava/util/HashMap;

    invoke-virtual {v0, v2}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_3

    invoke-virtual {v0, v2}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/util/LruCache;

    iput-object v0, v3, Ls/f;->c:Landroid/util/LruCache;

    goto :goto_2

    :cond_3
    new-instance v1, Landroid/util/LruCache;

    const/4 v4, 0x5

    invoke-direct {v1, v4}, Landroid/util/LruCache;-><init>(I)V

    iput-object v1, v3, Ls/f;->c:Landroid/util/LruCache;

    invoke-virtual {v0, v2, v1}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :goto_2
    invoke-virtual {p2, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_0

    :cond_4
    return-void
.end method


# virtual methods
.method public final a()Z
    .locals 3

    iget-object v0, p0, Ls/e;->c:Ls/h;

    if-nez v0, :cond_0

    const/4 v0, 0x0

    goto :goto_0

    :cond_0
    invoke-virtual {v0}, Ls/h;->f()Landroid/view/View;

    move-result-object v0

    :goto_0
    iget-object v1, p0, Ls/e;->e:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;

    const-string v2, "WebViewController"

    if-eqz v0, :cond_1

    iget-boolean v0, v1, Lr/b;->e:Z

    if-eqz v0, :cond_1

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "catchHtmlBody() mContentLoadingState="

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v1, p0, Ls/e;->a:Ls/e$a;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v2, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p0, p0, Ls/e;->c:Ls/h;

    const-string v0, "document.body.innerText"

    invoke-virtual {p0, v0}, Ls/h;->a(Ljava/lang/String;)V

    const/4 p0, 0x1

    return p0

    :cond_1
    new-instance p0, Ljava/lang/StringBuilder;

    const-string v0, "ignore, mIsWorking:"

    invoke-direct {p0, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-boolean v0, v1, Lr/b;->e:Z

    invoke-virtual {p0, v0}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v2, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    const/4 p0, 0x0

    return p0
.end method

.method public final b()V
    .locals 2

    iget-object v0, p0, Ls/e;->c:Ls/h;

    if-nez v0, :cond_0

    const/4 v0, 0x0

    goto :goto_0

    :cond_0
    invoke-virtual {v0}, Ls/h;->f()Landroid/view/View;

    move-result-object v0

    :goto_0
    if-eqz v0, :cond_1

    const-string v0, "WebViewController"

    const-string v1, "injectTouchEventToWebView"

    invoke-static {v0, v1}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "function xmcontentcatcherload(){\n    document.addEventListener(\'touchstart\',xmcontentcatchertouch, false);\n    document.addEventListener(\'touchmove\',xmcontentcatchertouch, false);\n    document.addEventListener(\'touchend\',xmcontentcatchertouch, false);\n    var timeOutEvent=0;\n    function xmcontentcatchertouch (event){\n        var event = event || window.event;\n        switch(event.type){\n            case \"touchstart\":\n                timeOutEvent = setTimeout(function(){\n                var target = event.target.nodeName;\n                var catchercontent = null;\n                if(target === \'IMG\'){\n                  catchercontent = \'img:\'+ event.target.getAttribute(\"src\");\n                }else{\n                  catchercontent =\'txt:\' + event.target.innerText;\n                }\nwindow.MiWebViewDetector.onCatch(\'"

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v1, p0, Ls/e;->b:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "@@\'+ catchercontent);}, "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p0, Ls/e;->e:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;

    iget v1, v1, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->n:I

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v1, ")\n                break;\n            case \"touchend\":\n            clearTimeout(timeOutEvent)\n                break;\n            case \"touchmove\":\n            clearTimeout(timeOutEvent)\n                break;\n        }\n    }\n}"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v0, "javascript:xmcontentcatcherload();"

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    iget-object p0, p0, Ls/e;->c:Ls/h;

    invoke-virtual {p0, v0}, Ls/h;->c(Ljava/lang/String;)V

    :cond_1
    return-void
.end method

.method public final c(Ljava/lang/String;)V
    .locals 4

    iget-object v0, p0, Ls/e;->e:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;

    iget-object v1, v0, Lr/b;->a:Ljava/util/LinkedHashMap;

    invoke-virtual {v1}, Ljava/util/LinkedHashMap;->clear()V

    iget-object v1, p0, Ls/e;->h:Ljava/util/ArrayList;

    invoke-virtual {v1}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :cond_0
    :goto_0
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_1

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ls/f;

    iget-object v3, p0, Ls/e;->j:Ljava/lang/String;

    invoke-virtual {v2, v3}, Ls/f;->a(Ljava/lang/String;)Z

    move-result v3

    if-eqz v3, :cond_0

    iget-object v3, v0, Lr/b;->a:Ljava/util/LinkedHashMap;

    iget-object v2, v2, Ls/f;->a:Ljava/lang/String;

    invoke-interface {v3, v2, p1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    goto :goto_0

    :cond_1
    iget-object p0, p0, Ls/e;->f:Lcom/miui/contentcatcher/Interceptor;

    if-eqz p0, :cond_2

    iget-object p1, v0, Lr/b;->a:Ljava/util/LinkedHashMap;

    invoke-interface {p1}, Ljava/util/Map;->isEmpty()Z

    move-result p1

    if-nez p1, :cond_2

    iget-object p1, v0, Lr/b;->a:Ljava/util/LinkedHashMap;

    const/4 v0, 0x1

    invoke-virtual {p0, v0, p1}, Lcom/miui/contentcatcher/Interceptor;->k(ILjava/util/LinkedHashMap;)V

    :cond_2
    return-void
.end method

.method public final d(Ljava/lang/String;)V
    .locals 8

    iget-object v0, p0, Ls/e;->c:Ls/h;

    const-string v1, "WebViewController"

    const-string v2, "newUrl: "

    const-string v3, "getUrl()-"

    const-string v4, "mContentLoadingState = "

    const-string v5, "value.length: "

    const-string v6, "webviewvalue-onCallback: "

    :try_start_0
    const-string v7, "WebViewCallback-onCallback"

    invoke-static {v1, v7}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    if-nez p1, :cond_0

    const-string p0, "onCallback is null"

    invoke-static {v1, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    return-void

    :catch_0
    move-exception p0

    goto/16 :goto_0

    :catch_1
    move-exception p0

    goto/16 :goto_1

    :cond_0
    invoke-virtual {v6, p1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v6

    invoke-static {v1, v6}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1}, Ljava/lang/String;->length()I

    move-result v5

    invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-static {v1, v5}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    const-string v5, "\"FUNCTION-EXIST: "

    invoke-virtual {p1, v5}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v5

    if-eqz v5, :cond_3

    const-string v0, "function check result"

    invoke-static {v1, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    const-string v0, "\"FUNCTION-EXIST: function"

    invoke-virtual {p1, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_2

    const-string v0, "\"FUNCTION-EXIST: function-function"

    invoke-virtual {p1, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result p1

    if-eqz p1, :cond_1

    const-string p0, "Existing xmcontentcatcherload, ignore inject"

    invoke-static {v1, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    goto/16 :goto_2

    :cond_1
    invoke-virtual {p0}, Ls/e;->e()V

    goto/16 :goto_2

    :cond_2
    const-string p0, "onCatch function not registered"

    invoke-static {v1, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    goto/16 :goto_2

    :cond_3
    const-string v5, "catch result"

    invoke-static {v1, v5}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v4, p0, Ls/e;->a:Ls/e$a;

    invoke-virtual {v5, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-static {v1, v4}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0}, Ls/h;->e()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v5

    if-nez v5, :cond_6

    invoke-static {p1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v5

    if-nez v5, :cond_6

    invoke-virtual {v0}, Ls/h;->d()Ljava/lang/String;

    move-result-object v5

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v6, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v3, ", getOriginalUrl()-"

    invoke-virtual {v6, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v1, v3}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v3, p0, Ls/e;->j:Ljava/lang/String;

    invoke-virtual {v4, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_5

    iput-object v4, p0, Ls/e;->j:Ljava/lang/String;

    invoke-virtual {v2, v4}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v2, p0, Ls/e;->a:Ls/e$a;

    sget-object v3, Ls/e$a;->c:Ls/e$a;

    if-eq v2, v3, :cond_4

    iput-object v3, p0, Ls/e;->a:Ls/e$a;

    iget-object v2, p0, Ls/e;->d:Ls/e$b;

    const/4 v3, 0x2

    invoke-virtual {v2, v3}, Landroid/os/Handler;->removeMessages(I)V

    :cond_4
    iget-boolean v2, p0, Ls/e;->l:Z

    if-eqz v2, :cond_5

    const-string v2, "checkJsDetector: "

    const-string v3, "\'FUNCTION-EXIST: \' + ((typeof MiWebViewDetector)==\'object\' ? (typeof MiWebViewDetector.onCatch) : \'objundefined\') + \'-\' + (typeof xmcontentcatcherload)"

    invoke-virtual {v2, v3}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0, v3}, Ls/h;->a(Ljava/lang/String;)V

    :cond_5
    iget-object v0, p0, Ls/e;->k:Ljava/lang/String;

    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_6

    iput-object p1, p0, Ls/e;->k:Ljava/lang/String;

    invoke-virtual {p0, p1}, Ls/e;->c(Ljava/lang/String;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_2

    :goto_0
    const-string p1, "onCallback-Error: "

    invoke-static {p1, p0, v1}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_2

    :goto_1
    const-string p1, "onCallback-Exception: "

    invoke-static {p1, p0, v1}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_6
    :goto_2
    return-void
.end method

.method public final e()V
    .locals 2

    iget-object v0, p0, Ls/e;->i:Ljava/util/ArrayList;

    if-eqz v0, :cond_1

    invoke-virtual {v0}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v0

    if-nez v0, :cond_1

    iget-object v0, p0, Ls/e;->c:Ls/h;

    if-nez v0, :cond_0

    const/4 v0, 0x0

    goto :goto_0

    :cond_0
    invoke-virtual {v0}, Ls/h;->f()Landroid/view/View;

    move-result-object v0

    :goto_0
    if-eqz v0, :cond_1

    const-string v0, "WebViewController"

    const-string v1, "requestInjectTouchEventDelay"

    invoke-static {v0, v1}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p0, p0, Ls/e;->d:Ls/e$b;

    const/4 v0, 0x3

    invoke-virtual {p0, v0}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    :cond_1
    return-void
.end method
