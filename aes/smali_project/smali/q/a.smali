.class public final Lq/a;
.super Lmiui/contentcatcher/sdk/injector/IContentDecorateCallback$Stub;
.source "SourceFile"


# instance fields
.field public a:Ljava/lang/ref/WeakReference;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/lang/ref/WeakReference<",
            "Lq/b;",
            ">;"
        }
    .end annotation
.end field


# virtual methods
.method public final updateContent(Lmiui/contentcatcher/sdk/DecorateContentParam;)V
    .locals 2
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Landroid/os/RemoteException;
        }
    .end annotation

    const-string v0, "ContentDecorateCallback"

    iget-object p0, p0, Lq/a;->a:Ljava/lang/ref/WeakReference;

    if-eqz p0, :cond_0

    :try_start_0
    invoke-virtual {p0}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v1
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    if-eqz v1, :cond_0

    :try_start_1
    invoke-virtual {p0}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Lq/b;

    invoke-interface {p0, p1}, Lq/b;->updateContent(Lmiui/contentcatcher/sdk/DecorateContentParam;)V
    :try_end_1
    .catch Ljava/lang/NullPointerException; {:try_start_1 .. :try_end_1} :catch_2
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1
    .catch Ljava/lang/Error; {:try_start_1 .. :try_end_1} :catch_0

    goto :goto_2

    :catch_0
    move-exception p0

    goto :goto_0

    :catch_1
    move-exception p0

    goto :goto_1

    :goto_0
    const-string p1, "updateContent-Error: "

    invoke-static {p1, p0, v0}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_2

    :goto_1
    const-string p1, "updateContent-Exception: "

    invoke-static {p1, p0, v0}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :catch_2
    :cond_0
    :goto_2
    return-void
.end method
