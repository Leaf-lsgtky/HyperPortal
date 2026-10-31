.class public final Lcom/miui/contentcatcher/Interceptor$b;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/lang/Runnable;


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lcom/miui/contentcatcher/Interceptor;->notifyWebView(Landroid/view/View;Z)V
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = null
.end annotation


# instance fields
.field public final synthetic a:Z

.field public final synthetic b:Landroid/view/View;

.field public final synthetic c:Lcom/miui/contentcatcher/Interceptor;


# direct methods
.method public constructor <init>(Lcom/miui/contentcatcher/Interceptor;ZLandroid/view/View;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()V"
        }
    .end annotation

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/miui/contentcatcher/Interceptor$b;->c:Lcom/miui/contentcatcher/Interceptor;

    iput-boolean p2, p0, Lcom/miui/contentcatcher/Interceptor$b;->a:Z

    iput-object p3, p0, Lcom/miui/contentcatcher/Interceptor$b;->b:Landroid/view/View;

    return-void
.end method


# virtual methods
.method public final run()V
    .locals 5

    iget-boolean v0, p0, Lcom/miui/contentcatcher/Interceptor$b;->a:Z

    if-eqz v0, :cond_0

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor$b;->c:Lcom/miui/contentcatcher/Interceptor;

    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor$b;->b:Landroid/view/View;

    monitor-enter v0

    :try_start_0
    iget-object v2, v0, Lcom/miui/contentcatcher/Interceptor;->o:Ljava/util/LinkedList;

    new-instance v3, Ljava/lang/ref/WeakReference;

    invoke-direct {v3, v1}, Ljava/lang/ref/WeakReference;-><init>(Ljava/lang/Object;)V

    invoke-virtual {v2, v3}, Ljava/util/LinkedList;->add(Ljava/lang/Object;)Z
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    monitor-exit v0

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor$b;->c:Lcom/miui/contentcatcher/Interceptor;

    iget-object v0, v0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor$b;->b:Landroid/view/View;

    iget-object v0, v0, Ld/c;->a:Ljava/io/Serializable;

    check-cast v0, Ljava/util/ArrayList;

    invoke-virtual {v0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :goto_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_3

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lr/b;

    const/4 v3, 0x1

    invoke-virtual {v2, v1, v3}, Lr/b;->h(Landroid/view/View;Z)V

    goto :goto_0

    :catchall_0
    move-exception p0

    :try_start_1
    monitor-exit v0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    throw p0

    :cond_0
    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor$b;->c:Lcom/miui/contentcatcher/Interceptor;

    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor$b;->b:Landroid/view/View;

    monitor-enter v0

    :try_start_2
    iget-object v2, v0, Lcom/miui/contentcatcher/Interceptor;->o:Ljava/util/LinkedList;

    invoke-virtual {v2}, Ljava/util/AbstractCollection;->iterator()Ljava/util/Iterator;

    move-result-object v2

    :cond_1
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    move-result v3

    if-eqz v3, :cond_2

    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/ref/WeakReference;

    invoke-virtual {v3}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Landroid/view/View;

    if-ne v4, v1, :cond_1

    iget-object v1, v0, Lcom/miui/contentcatcher/Interceptor;->o:Ljava/util/LinkedList;

    invoke-virtual {v1, v3}, Ljava/util/LinkedList;->remove(Ljava/lang/Object;)Z
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    goto :goto_1

    :catchall_1
    move-exception p0

    goto :goto_3

    :cond_2
    :goto_1
    monitor-exit v0

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor$b;->c:Lcom/miui/contentcatcher/Interceptor;

    iget-object v0, v0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor$b;->b:Landroid/view/View;

    iget-object v0, v0, Ld/c;->a:Ljava/io/Serializable;

    check-cast v0, Ljava/util/ArrayList;

    invoke-virtual {v0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :goto_2
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_3

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lr/b;

    const/4 v3, 0x0

    invoke-virtual {v2, v1, v3}, Lr/b;->h(Landroid/view/View;Z)V

    goto :goto_2

    :cond_3
    const-string v0, "Interceptor_Catcher"

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "notifyWebView view: "

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor$b;->b:Landroid/view/View;

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v2, ", size:"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor$b;->c:Lcom/miui/contentcatcher/Interceptor;

    iget-object v2, v2, Lcom/miui/contentcatcher/Interceptor;->o:Ljava/util/LinkedList;

    invoke-virtual {v2}, Ljava/util/LinkedList;->size()I

    move-result v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v2, ", isLoad:"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-boolean v2, p0, Lcom/miui/contentcatcher/Interceptor$b;->a:Z

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v0, v1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    sget-boolean v0, Lp/b;->a:Z

    if-eqz v0, :cond_4

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor$b;->c:Lcom/miui/contentcatcher/Interceptor;

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->p()V

    :cond_4
    return-void

    :goto_3
    :try_start_3
    monitor-exit v0
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_1

    throw p0
.end method
