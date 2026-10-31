.class public final synthetic Lz/f;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/lang/Runnable;


# instance fields
.field public final synthetic a:Lz/j;

.field public final synthetic b:Landroid/view/View;

.field public final synthetic c:Lz/j$c;


# direct methods
.method public synthetic constructor <init>(Lz/j;Landroid/view/View;Lz/j$c;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lz/f;->a:Lz/j;

    iput-object p2, p0, Lz/f;->b:Landroid/view/View;

    iput-object p3, p0, Lz/f;->c:Lz/j$c;

    return-void
.end method


# virtual methods
.method public final run()V
    .locals 9

    iget-object v0, p0, Lz/f;->a:Lz/j;

    iget-object v1, p0, Lz/f;->b:Landroid/view/View;

    iget-object p0, p0, Lz/f;->c:Lz/j$c;

    const-string v2, "limit "

    iget-boolean v3, v0, Lz/j;->b:Z

    if-nez v3, :cond_2

    sget-object v3, Lz/j;->d:Lz/j$b;

    sget-object v4, Lz/j$b;->b:Lz/j$b;

    if-eq v3, v4, :cond_0

    goto/16 :goto_4

    :cond_0
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v3

    const-string v5, "Catcher dumpViews"

    invoke-static {v5}, Landroid/os/Trace;->beginSection(Ljava/lang/String;)V

    :try_start_0
    new-instance v5, Lz/l;

    invoke-direct {v5}, Lz/l;-><init>()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_1

    :try_start_1
    invoke-static {v1}, Lmiui/contentcatcher/InterceptorHideFunctionHelper;->getViewGlobalTransformMatrix(Landroid/view/View;)Landroid/graphics/Matrix;

    move-result-object v6
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    goto :goto_0

    :catchall_0
    const/4 v6, 0x0

    :goto_0
    if-nez v6, :cond_1

    :try_start_2
    sget-object v6, Landroid/graphics/Matrix;->IDENTITY_MATRIX:Landroid/graphics/Matrix;

    goto :goto_1

    :catchall_1
    move-exception p0

    goto :goto_3

    :cond_1
    :goto_1
    new-instance v7, Lz/j$a;

    invoke-direct {v7}, Ljava/lang/Object;-><init>()V

    iput-object v6, v7, Lz/j$a;->a:Landroid/graphics/Matrix;

    iget-object v6, p0, Lz/j$c;->i:Landroid/graphics/Rect;

    iput-object v6, v7, Lz/j$a;->b:Landroid/graphics/Rect;

    iput-wide v3, v7, Lz/j$a;->c:J

    iget-object v3, p0, Lz/j$c;->d:Landroid/os/Bundle;

    const-string v4, "max_depth"

    const/16 v6, 0x1e

    invoke-static {v3, v4, v6}, Lz/k;->e(Landroid/os/Bundle;Ljava/lang/String;I)I

    move-result v3

    iput v3, v7, Lz/j$a;->d:I

    const/4 v4, 0x0

    invoke-static {v4, v3}, Ljava/lang/Math;->max(II)I

    move-result v3

    iput v3, v7, Lz/j$a;->d:I

    iget-object v3, p0, Lz/j$c;->d:Landroid/os/Bundle;

    const-string v6, "max_process_time"

    const/16 v8, 0x8

    invoke-static {v3, v6, v8}, Lz/k;->e(Landroid/os/Bundle;Ljava/lang/String;I)I

    move-result v3

    iput v3, v7, Lz/j$a;->e:I

    invoke-static {v4, v3}, Ljava/lang/Math;->max(II)I

    move-result v3

    iput v3, v7, Lz/j$a;->e:I

    iget-object v3, p0, Lz/j$c;->d:Landroid/os/Bundle;

    invoke-static {v3}, Lz/k;->d(Landroid/os/Bundle;)V

    const-string v3, "Catcher_AICollectorManager"

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget v2, v7, Lz/j$a;->d:I

    invoke-virtual {v6, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v2, "/"

    invoke-virtual {v6, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v2, v7, Lz/j$a;->e:I

    invoke-virtual {v6, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v3, v2}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {v1, v5, v7, v4}, Lz/j;->k(Landroid/view/View;Lz/l;Lz/j$a;I)Z

    iget-object v1, v0, Lz/j;->a:Landroid/os/Handler;

    const/4 v2, 0x3

    invoke-virtual {v1, v2}, Landroid/os/Handler;->obtainMessage(I)Landroid/os/Message;

    move-result-object v1

    iput-object p0, v1, Landroid/os/Message;->obj:Ljava/lang/Object;

    iput-object v5, p0, Lz/j$c;->j:Ljava/lang/Object;

    invoke-virtual {v1}, Landroid/os/Message;->sendToTarget()V
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_0
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    :goto_2
    invoke-static {}, Landroid/os/Trace;->endSection()V

    goto :goto_4

    :catch_0
    :try_start_3
    iget-object v1, v0, Lz/j;->a:Landroid/os/Handler;

    const/4 v2, 0x2

    invoke-virtual {v1, v2}, Landroid/os/Handler;->obtainMessage(I)Landroid/os/Message;

    move-result-object v1

    iput-object p0, v1, Landroid/os/Message;->obj:Ljava/lang/Object;

    iget-object p0, v0, Lz/j;->a:Landroid/os/Handler;

    invoke-virtual {p0, v1}, Landroid/os/Handler;->sendMessage(Landroid/os/Message;)Z
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_1

    goto :goto_2

    :goto_3
    invoke-static {}, Landroid/os/Trace;->endSection()V

    throw p0

    :cond_2
    :goto_4
    return-void
.end method
