.class public final Lu/a$a;
.super Landroid/os/Handler;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lu/a;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x11
    name = "a"
.end annotation


# instance fields
.field public final synthetic a:Lu/a;


# direct methods
.method public constructor <init>(Lu/a;)V
    .locals 0

    iput-object p1, p0, Lu/a$a;->a:Lu/a;

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    move-result-object p1

    invoke-direct {p0, p1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    return-void
.end method


# virtual methods
.method public final handleMessage(Landroid/os/Message;)V
    .locals 4

    const-string v0, "CatcherInputEventListener"

    iget v1, p1, Landroid/os/Message;->what:I

    const/4 v2, 0x1

    if-eq v1, v2, :cond_0

    goto :goto_2

    :cond_0
    :try_start_0
    iget-object p0, p0, Lu/a$a;->a:Lu/a;

    iput-boolean v2, p0, Lu/a;->i:Z

    iget-object p1, p1, Landroid/os/Message;->obj:Ljava/lang/Object;

    check-cast p1, Landroid/graphics/PointF;

    iget-object v1, p0, Lu/a;->h:Lcom/miui/contentcatcher/Interceptor;

    if-eqz v1, :cond_1

    iget-object v2, p0, Lu/a;->q:Landroid/view/View;

    if-eqz v2, :cond_1

    iget-boolean v3, p0, Lu/a;->r:Z

    if-eqz v3, :cond_1

    iget-object v3, p0, Lu/a;->m:Landroid/graphics/PointF;

    invoke-virtual {v1, p1, v3, v2}, Lcom/miui/contentcatcher/Interceptor;->l(Landroid/graphics/PointF;Landroid/graphics/PointF;Landroid/view/View;)Z

    move-result p1

    iput-boolean p1, p0, Lu/a;->j:Z
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_2

    :catch_0
    move-exception p0

    goto :goto_0

    :catch_1
    move-exception p0

    goto :goto_1

    :goto_0
    const-string p1, "MSG_SINGLE_LONG_PRESS-Error: "

    invoke-static {p1, p0, v0}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_2

    :goto_1
    const-string p1, "MSG_SINGLE_LONG_PRESS-Exception: "

    invoke-static {p1, p0, v0}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_1
    :goto_2
    return-void
.end method
