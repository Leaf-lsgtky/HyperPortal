.class public final Lm/b;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Landroid/webkit/ValueCallback;


# annotations
.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Object;",
        "Landroid/webkit/ValueCallback<",
        "Ljava/lang/String;",
        ">;"
    }
.end annotation


# instance fields
.field public final synthetic a:Lcom/miui/contentcatcher/Interceptor;


# direct methods
.method public constructor <init>(Lcom/miui/contentcatcher/Interceptor;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lm/b;->a:Lcom/miui/contentcatcher/Interceptor;

    return-void
.end method


# virtual methods
.method public final onReceiveValue(Ljava/lang/Object;)V
    .locals 3

    check-cast p1, Ljava/lang/String;

    iget-object p0, p0, Lm/b;->a:Lcom/miui/contentcatcher/Interceptor;

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->u:Ly/g;

    iget-object p0, p0, Ly/g;->g:Ly/g$a;

    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    const-string v0, "SettingTrigger"

    const-string v1, ">>>>>>>>>>> fetch=>onFetchWebViewUrl"

    invoke-static {v0, v1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, ">>>>>>>>>>> fetch=>onFetchWebViewUrl, web url:"

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v0, v1}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {p1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v1

    iget-object p0, p0, Ly/g$a;->a:Ly/g;

    if-nez v1, :cond_0

    new-instance v1, Lcom/miui/contentcatcher/common/NodeInfo;

    invoke-direct {v1}, Lcom/miui/contentcatcher/common/NodeInfo;-><init>()V

    invoke-virtual {v1, p1}, Lcom/miui/contentcatcher/common/NodeInfo;->setWebviewUrl(Ljava/lang/String;)V

    const-string p1, "webview.url"

    invoke-virtual {v1, p1}, Lcom/miui/contentcatcher/common/NodeInfo;->setClazz(Ljava/lang/String;)V

    const/4 p1, 0x3

    invoke-virtual {v1, p1}, Lcom/miui/contentcatcher/common/NodeInfo;->setType(I)V

    new-instance p1, Ljava/lang/StringBuilder;

    const-string v2, "onFetchWebViewUrl add node size: 1 for uuid: "

    invoke-direct {p1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1}, Lcom/miui/contentcatcher/common/NodeInfo;->getUuid()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {p1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v0, p1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p1, p0, Ly/g;->c:Ljava/util/concurrent/CopyOnWriteArrayList;

    invoke-virtual {p1, v1}, Ljava/util/concurrent/CopyOnWriteArrayList;->add(Ljava/lang/Object;)Z

    :cond_0
    iget-object p1, p0, Ly/g;->e:Ljava/util/concurrent/atomic/AtomicInteger;

    invoke-virtual {p1}, Ljava/util/concurrent/atomic/AtomicInteger;->decrementAndGet()I

    new-instance p1, Ljava/lang/StringBuilder;

    const-string v1, "onFetchWebViewUrl countDown: "

    invoke-direct {p1, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v1, p0, Ly/g;->e:Ljava/util/concurrent/atomic/AtomicInteger;

    invoke-virtual {v1}, Ljava/util/concurrent/atomic/AtomicInteger;->intValue()I

    move-result v1

    invoke-virtual {p1, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v0, p1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p1, p0, Ly/g;->e:Ljava/util/concurrent/atomic/AtomicInteger;

    invoke-virtual {p1}, Ljava/util/concurrent/atomic/AtomicInteger;->intValue()I

    move-result p1

    if-nez p1, :cond_1

    iget-object p1, p0, Ly/g;->d:Landroid/os/Handler;

    iget-object p0, p0, Ly/g;->h:Ly/g$e;

    invoke-virtual {p1, p0}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    invoke-virtual {p1, p0}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    :cond_1
    return-void
.end method
