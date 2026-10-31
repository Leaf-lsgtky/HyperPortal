.class public final Ld/c;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ld/n;


# instance fields
.field public a:Ljava/io/Serializable;


# virtual methods
.method public a()Ljava/lang/Object;
    .locals 4

    iget-object p0, p0, Ld/c;->a:Ljava/io/Serializable;

    check-cast p0, Ljava/lang/Class;

    :try_start_0
    sget-object v0, Ld/t;->a:Ld/t;

    invoke-virtual {v0, p0}, Ld/t;->a(Ljava/lang/Class;)Ljava/lang/Object;

    move-result-object p0
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return-object p0

    :catch_0
    move-exception v0

    new-instance v1, Ljava/lang/RuntimeException;

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "Unable to create instance of "

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string p0, ". Registering an InstanceCreator or a TypeAdapter for this type, or adding a no-args constructor may fix this problem."

    invoke-virtual {v2, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-direct {v1, p0, v0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;Ljava/lang/Throwable;)V

    throw v1
.end method

.method public b(ILmiui/contentcatcher/sdk/Token;Ljava/util/HashMap;)V
    .locals 4

    iget-object v0, p0, Ld/c;->a:Ljava/io/Serializable;

    check-cast v0, Ljava/util/ArrayList;

    invoke-virtual {v0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :cond_0
    :goto_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_2

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lr/b;

    invoke-virtual {v1}, Lr/b;->g()Z

    move-result v2

    if-eqz v2, :cond_0

    iget-object v2, v1, Lr/b;->a:Ljava/util/LinkedHashMap;

    invoke-interface {v2}, Ljava/util/Map;->isEmpty()Z

    move-result v3

    if-nez v3, :cond_0

    if-eqz p3, :cond_1

    invoke-interface {v2, p3}, Ljava/util/Map;->putAll(Ljava/util/Map;)V

    :cond_1
    invoke-interface {v2}, Ljava/util/Map;->isEmpty()Z

    move-result v3

    if-nez v3, :cond_0

    invoke-virtual {v1}, Lr/b;->e()Ljava/lang/String;

    move-result-object v1

    new-instance v3, Ljava/util/HashMap;

    invoke-direct {v3}, Ljava/util/HashMap;-><init>()V

    invoke-virtual {v3, v2}, Ljava/util/HashMap;->putAll(Ljava/util/Map;)V

    new-instance v2, Lmiui/contentcatcher/sdk/Content;

    invoke-direct {v2, p1, v1, p2, v3}, Lmiui/contentcatcher/sdk/Content;-><init>(ILjava/lang/String;Lmiui/contentcatcher/sdk/Token;Ljava/util/Map;)V

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v3, "dispatchContent, content: "

    invoke-direct {v1, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    const-string v3, "CatcherHolder"

    invoke-static {v3, v1}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {v2}, Ldev/dragshare/catcher/Bridge;->deliverContent(Ljava/lang/Object;)V

    goto :goto_0

    :cond_2
    invoke-virtual {p0}, Ld/c;->g()V

    return-void
.end method

.method public c()Z
    .locals 2

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "mCatcherList size: "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object p0, p0, Ld/c;->a:Ljava/io/Serializable;

    check-cast p0, Ljava/util/ArrayList;

    invoke-virtual {p0}, Ljava/util/ArrayList;->size()I

    move-result v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "CatcherHolder"

    invoke-static {v1, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/util/ArrayList;->isEmpty()Z

    move-result p0

    xor-int/lit8 p0, p0, 0x1

    return p0
.end method

.method public d()Z
    .locals 2

    iget-object p0, p0, Ld/c;->a:Ljava/io/Serializable;

    check-cast p0, Ljava/util/ArrayList;

    invoke-virtual {p0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :cond_0
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_1

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lr/b;

    const/4 v1, 0x1

    invoke-virtual {v0, v1}, Lr/b;->f(I)Z

    move-result v0

    if-eqz v0, :cond_0

    return v1

    :cond_1
    const/4 p0, 0x0

    return p0
.end method

.method public e()V
    .locals 4

    iget-object p0, p0, Ld/c;->a:Ljava/io/Serializable;

    check-cast p0, Ljava/util/ArrayList;

    invoke-virtual {p0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :goto_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_0

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lr/b;

    invoke-virtual {v1}, Lr/b;->k()V

    iget-object v2, v1, Lr/b;->k:Ljava/lang/Object;

    monitor-enter v2

    const/4 v3, 0x0

    :try_start_0
    iput-object v3, v1, Lr/b;->d:Lcom/miui/contentcatcher/Interceptor;

    monitor-exit v2

    goto :goto_0

    :catchall_0
    move-exception p0

    monitor-exit v2
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw p0

    :cond_0
    invoke-virtual {p0}, Ljava/util/ArrayList;->clear()V

    return-void
.end method

.method public f(Landroid/os/Bundle;)V
    .locals 3

    invoke-virtual {p0}, Ld/c;->g()V

    const-string v0, "catchType"

    const/4 v1, 0x0

    invoke-virtual {p1, v0, v1}, Landroid/os/Bundle;->getInt(Ljava/lang/String;I)I

    move-result v0

    iget-object p0, p0, Ld/c;->a:Ljava/io/Serializable;

    check-cast p0, Ljava/util/ArrayList;

    invoke-virtual {p0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :cond_0
    :goto_0
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_1

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lr/b;

    invoke-virtual {v1}, Lr/b;->c()I

    move-result v2

    and-int/2addr v2, v0

    if-eqz v2, :cond_0

    invoke-virtual {v1, p1}, Lr/b;->a(Landroid/os/Bundle;)V

    goto :goto_0

    :cond_1
    return-void
.end method

.method public g()V
    .locals 1

    iget-object p0, p0, Ld/c;->a:Ljava/io/Serializable;

    check-cast p0, Ljava/util/ArrayList;

    invoke-virtual {p0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :goto_0
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lr/b;

    invoke-virtual {v0}, Lr/b;->n()V

    goto :goto_0

    :cond_0
    return-void
.end method

.method public h(Landroid/view/View;Ljava/util/HashSet;)V
    .locals 4

    const/4 v0, 0x1

    sget-object v1, Lv/c;->a:Ljava/util/HashSet;

    if-eqz p1, :cond_8

    invoke-virtual {p1}, Landroid/view/View;->getVisibility()I

    move-result v1

    if-eqz v1, :cond_0

    goto :goto_3

    :cond_0
    invoke-virtual {p1}, Landroid/view/View;->getWidth()I

    move-result v1

    const/4 v2, 0x2

    if-le v1, v2, :cond_8

    invoke-virtual {p1}, Landroid/view/View;->getHeight()I

    move-result v1

    if-gt v1, v2, :cond_1

    goto :goto_3

    :cond_1
    instance-of v1, p1, Landroid/widget/ImageView;

    if-eqz v1, :cond_2

    return-void

    :cond_2
    instance-of v1, p1, Landroid/view/ViewGroup;

    if-eqz v1, :cond_4

    move-object v1, p1

    check-cast v1, Landroid/view/ViewGroup;

    invoke-virtual {p2, p1}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_3

    return-void

    :cond_3
    invoke-virtual {v1}, Landroid/view/ViewGroup;->getChildCount()I

    move-result p1

    const/4 v2, 0x0

    :goto_0
    if-ge v2, p1, :cond_8

    invoke-virtual {v1, v2}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v3

    invoke-virtual {p0, v3, p2}, Ld/c;->h(Landroid/view/View;Ljava/util/HashSet;)V

    add-int/2addr v2, v0

    goto :goto_0

    :cond_4
    iget-object p0, p0, Ld/c;->a:Ljava/io/Serializable;

    check-cast p0, Ljava/util/ArrayList;

    invoke-virtual {p0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :cond_5
    :goto_1
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result p2

    if-eqz p2, :cond_8

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Lr/b;

    invoke-virtual {p2}, Lr/b;->d()I

    move-result v1

    and-int/2addr v1, v2

    if-eqz v1, :cond_5

    invoke-virtual {p2, v0}, Lr/b;->f(I)Z

    move-result v1

    if-nez v1, :cond_6

    goto :goto_1

    :cond_6
    if-nez p1, :cond_7

    move v1, v0

    goto :goto_2

    :cond_7
    invoke-virtual {p2, p1}, Lr/b;->b(Landroid/view/View;)Z

    move-result v1

    :goto_2
    if-eqz v1, :cond_5

    instance-of p2, p2, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;

    if-eqz p2, :cond_5

    :cond_8
    :goto_3
    return-void
.end method
