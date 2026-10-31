.class public final Ls/e$b;
.super Landroid/os/Handler;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Ls/e;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x9
    name = "b"
.end annotation


# instance fields
.field public a:Ljava/lang/ref/WeakReference;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/lang/ref/WeakReference<",
            "Ls/e;",
            ">;"
        }
    .end annotation
.end field


# virtual methods
.method public final handleMessage(Landroid/os/Message;)V
    .locals 6

    iget-object v0, p0, Ls/e$b;->a:Ljava/lang/ref/WeakReference;

    if-nez v0, :cond_0

    return-void

    :cond_0
    invoke-virtual {v0}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ls/e;

    if-nez v0, :cond_1

    return-void

    :cond_1
    iget p1, p1, Landroid/os/Message;->what:I

    const/4 v1, 0x1

    const-string v2, "WebViewController"

    if-eq p1, v1, :cond_5

    const/4 v3, 0x2

    if-eq p1, v3, :cond_3

    const/4 p0, 0x3

    if-eq p1, p0, :cond_2

    return-void

    :cond_2
    :try_start_0
    invoke-virtual {v0}, Ls/e;->b()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p0

    const-string p1, "MSG_INJECT_TOUCH_EVENT-Error: "

    invoke-static {p1, p0, v2}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_0

    :catch_1
    move-exception p0

    const-string p1, "MSG_INJECT_TOUCH_EVENT-Exception: "

    invoke-static {p1, p0, v2}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :goto_0
    return-void

    :cond_3
    :try_start_1
    invoke-virtual {v0}, Ls/e;->a()Z

    move-result p1

    if-eqz p1, :cond_4

    iget p1, v0, Ls/e;->g:I

    add-int/lit8 v4, p1, 0x1

    iput v4, v0, Ls/e;->g:I

    const/16 v0, 0xa

    if-ge p1, v0, :cond_4

    const-wide/16 v4, 0x5dc

    invoke-virtual {p0, v3, v4, v5}, Landroid/os/Handler;->sendEmptyMessageDelayed(IJ)Z
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_3
    .catch Ljava/lang/Error; {:try_start_1 .. :try_end_1} :catch_2

    goto :goto_3

    :catch_2
    move-exception p0

    goto :goto_1

    :catch_3
    move-exception p0

    goto :goto_2

    :goto_1
    const-string p1, "MSG_REPEAT_GET_HTML-Error: "

    invoke-static {p1, p0, v2}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_3

    :goto_2
    const-string p1, "MSG_REPEAT_GET_HTML-Exception: "

    invoke-static {p1, p0, v2}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_4
    :goto_3
    sput-boolean v1, Ls/e;->m:Z

    const-string p0, "sJsInject true"

    invoke-static {v2, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    return-void

    :cond_5
    :try_start_2
    invoke-virtual {v0}, Ls/e;->a()Z
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_5
    .catch Ljava/lang/Error; {:try_start_2 .. :try_end_2} :catch_4

    goto :goto_4

    :catch_4
    move-exception p0

    const-string p1, "MSG_GET_HTML_BODY-Error: "

    invoke-static {p1, p0, v2}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_4

    :catch_5
    move-exception p0

    const-string p1, "MSG_GET_HTML_BODY-Exception: "

    invoke-static {p1, p0, v2}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :goto_4
    return-void
.end method
