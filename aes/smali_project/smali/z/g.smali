.class public final synthetic Lz/g;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/lang/Runnable;


# instance fields
.field public final synthetic a:Lz/j;

.field public final synthetic b:Landroid/view/View;

.field public final synthetic c:Lz/j$c;

.field public final synthetic d:Ljava/util/ArrayList;


# direct methods
.method public synthetic constructor <init>(Lz/j;Landroid/view/View;Lz/j$c;Ljava/util/ArrayList;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lz/g;->a:Lz/j;

    iput-object p2, p0, Lz/g;->b:Landroid/view/View;

    iput-object p3, p0, Lz/g;->c:Lz/j$c;

    iput-object p4, p0, Lz/g;->d:Ljava/util/ArrayList;

    return-void
.end method


# virtual methods
.method public final run()V
    .locals 9

    iget-object v0, p0, Lz/g;->a:Lz/j;

    iget-object v1, p0, Lz/g;->b:Landroid/view/View;

    iget-object v2, p0, Lz/g;->c:Lz/j$c;

    iget-object p0, p0, Lz/g;->d:Ljava/util/ArrayList;

    const-string v3, "limit "

    iget-boolean v4, v0, Lz/j;->b:Z

    if-nez v4, :cond_2

    sget-object v4, Lz/j;->d:Lz/j$b;

    sget-object v5, Lz/j$b;->b:Lz/j$b;

    if-eq v4, v5, :cond_0

    goto/16 :goto_4

    :cond_0
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v4

    const-string v6, "Catcher findNodes"

    invoke-static {v6}, Landroid/os/Trace;->beginSection(Ljava/lang/String;)V

    :try_start_0
    invoke-static {v1}, Lmiui/contentcatcher/InterceptorHideFunctionHelper;->getViewGlobalTransformMatrix(Landroid/view/View;)Landroid/graphics/Matrix;

    move-result-object v6
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    goto :goto_0

    :catchall_0
    const/4 v6, 0x0

    :goto_0
    if-nez v6, :cond_1

    :try_start_1
    sget-object v6, Landroid/graphics/Matrix;->IDENTITY_MATRIX:Landroid/graphics/Matrix;

    goto :goto_1

    :catchall_1
    move-exception p0

    goto/16 :goto_3

    :cond_1
    :goto_1
    new-instance v7, Lz/j$a;

    invoke-direct {v7}, Ljava/lang/Object;-><init>()V

    iput-object v6, v7, Lz/j$a;->a:Landroid/graphics/Matrix;

    iget-object v6, v2, Lz/j$c;->i:Landroid/graphics/Rect;

    iput-object v6, v7, Lz/j$a;->b:Landroid/graphics/Rect;

    iput-wide v4, v7, Lz/j$a;->c:J

    iget-object v4, v2, Lz/j$c;->d:Landroid/os/Bundle;

    const-string v5, "max_depth"

    const/16 v6, 0x1e

    invoke-static {v4, v5, v6}, Lz/k;->e(Landroid/os/Bundle;Ljava/lang/String;I)I

    move-result v4

    iput v4, v7, Lz/j$a;->d:I

    const/4 v5, 0x0

    invoke-static {v5, v4}, Ljava/lang/Math;->max(II)I

    move-result v4

    iput v4, v7, Lz/j$a;->d:I

    iget-object v4, v2, Lz/j$c;->d:Landroid/os/Bundle;

    const-string v6, "max_process_time"

    const/16 v8, 0x8

    invoke-static {v4, v6, v8}, Lz/k;->e(Landroid/os/Bundle;Ljava/lang/String;I)I

    move-result v4

    iput v4, v7, Lz/j$a;->e:I

    invoke-static {v5, v4}, Ljava/lang/Math;->max(II)I

    move-result v4

    iput v4, v7, Lz/j$a;->e:I

    const-string v4, "Catcher_AICollectorManager"

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget v3, v7, Lz/j$a;->d:I

    invoke-virtual {v6, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v3, "/"

    invoke-virtual {v6, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v3, v7, Lz/j$a;->e:I

    invoke-virtual {v6, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v4, v3}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v3, v2, Lz/j$c;->d:Landroid/os/Bundle;

    invoke-static {v3}, Lz/k;->d(Landroid/os/Bundle;)V

    new-instance v3, Ljava/util/ArrayList;

    invoke-direct {v3}, Ljava/util/ArrayList;-><init>()V

    invoke-static {v1, v3, v7, p0, v5}, Lz/j;->e(Landroid/view/View;Ljava/util/ArrayList;Lz/j$a;Ljava/util/List;I)Z

    iget-object p0, v0, Lz/j;->a:Landroid/os/Handler;

    const/4 v1, 0x4

    invoke-virtual {p0, v1}, Landroid/os/Handler;->obtainMessage(I)Landroid/os/Message;

    move-result-object p0

    iput-object v3, v2, Lz/j$c;->j:Ljava/lang/Object;

    iput-object v2, p0, Landroid/os/Message;->obj:Ljava/lang/Object;

    invoke-virtual {p0}, Landroid/os/Message;->sendToTarget()V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_0
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    :goto_2
    invoke-static {}, Landroid/os/Trace;->endSection()V

    goto :goto_4

    :catch_0
    :try_start_2
    iget-object p0, v0, Lz/j;->a:Landroid/os/Handler;

    const/4 v1, 0x2

    invoke-virtual {p0, v1}, Landroid/os/Handler;->obtainMessage(I)Landroid/os/Message;

    move-result-object p0

    iput-object v2, p0, Landroid/os/Message;->obj:Ljava/lang/Object;

    iget-object v0, v0, Lz/j;->a:Landroid/os/Handler;

    invoke-virtual {v0, p0}, Landroid/os/Handler;->sendMessage(Landroid/os/Message;)Z
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    goto :goto_2

    :goto_3
    invoke-static {}, Landroid/os/Trace;->endSection()V

    throw p0

    :cond_2
    :goto_4
    return-void
.end method
