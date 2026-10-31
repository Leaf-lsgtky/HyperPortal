.class public final Le/o$t;
.super Lcom/google/gson/s;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Le/o;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = null
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Lcom/google/gson/s<",
        "Lcom/google/gson/h;",
        ">;"
    }
.end annotation


# direct methods
.method public static c(Lj/a;Lj/b;)Lcom/google/gson/h;
    .locals 2
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    invoke-virtual {p1}, Ljava/lang/Enum;->ordinal()I

    move-result v0

    const/4 v1, 0x5

    if-eq v0, v1, :cond_3

    const/4 v1, 0x6

    if-eq v0, v1, :cond_2

    const/4 v1, 0x7

    if-eq v0, v1, :cond_1

    const/16 v1, 0x8

    if-ne v0, v1, :cond_0

    invoke-virtual {p0}, Lj/a;->t()V

    sget-object p0, Lcom/google/gson/j;->a:Lcom/google/gson/j;

    return-object p0

    :cond_0
    new-instance p0, Ljava/lang/IllegalStateException;

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "Unexpected token: "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p0

    :cond_1
    new-instance p1, Lcom/google/gson/m;

    invoke-virtual {p0}, Lj/a;->n()Z

    move-result p0

    invoke-static {p0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object p0

    invoke-direct {p1, p0}, Lcom/google/gson/m;-><init>(Ljava/lang/Boolean;)V

    return-object p1

    :cond_2
    invoke-virtual {p0}, Lj/a;->v()Ljava/lang/String;

    move-result-object p0

    new-instance p1, Lcom/google/gson/m;

    new-instance v0, Ld/l;

    invoke-direct {v0, p0}, Ld/l;-><init>(Ljava/lang/String;)V

    invoke-direct {p1, v0}, Lcom/google/gson/m;-><init>(Ljava/lang/Number;)V

    return-object p1

    :cond_3
    new-instance p1, Lcom/google/gson/m;

    invoke-virtual {p0}, Lj/a;->v()Ljava/lang/String;

    move-result-object p0

    invoke-direct {p1, p0}, Lcom/google/gson/m;-><init>(Ljava/lang/String;)V

    return-object p1
.end method

.method public static d(Lj/c;Lcom/google/gson/h;)V
    .locals 2
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    if-eqz p1, :cond_b

    instance-of v0, p1, Lcom/google/gson/j;

    if-eqz v0, :cond_0

    goto/16 :goto_2

    :cond_0
    instance-of v0, p1, Lcom/google/gson/m;

    if-eqz v0, :cond_4

    if-eqz v0, :cond_3

    check-cast p1, Lcom/google/gson/m;

    iget-object v0, p1, Lcom/google/gson/m;->a:Ljava/io/Serializable;

    instance-of v1, v0, Ljava/lang/Number;

    if-eqz v1, :cond_1

    invoke-virtual {p1}, Lcom/google/gson/m;->b()Ljava/lang/Number;

    move-result-object p1

    invoke-virtual {p0, p1}, Lj/c;->n(Ljava/lang/Number;)V

    goto/16 :goto_3

    :cond_1
    instance-of v0, v0, Ljava/lang/Boolean;

    if-eqz v0, :cond_2

    invoke-virtual {p1}, Lcom/google/gson/m;->a()Z

    move-result p1

    invoke-virtual {p0, p1}, Lj/c;->p(Z)V

    goto/16 :goto_3

    :cond_2
    invoke-virtual {p1}, Lcom/google/gson/m;->c()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {p0, p1}, Lj/c;->o(Ljava/lang/String;)V

    goto/16 :goto_3

    :cond_3
    new-instance p0, Ljava/lang/IllegalStateException;

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "Not a JSON Primitive: "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p0

    :cond_4
    instance-of v0, p1, Lcom/google/gson/f;

    if-eqz v0, :cond_7

    invoke-virtual {p0}, Lj/c;->b()V

    if-eqz v0, :cond_6

    check-cast p1, Lcom/google/gson/f;

    iget-object p1, p1, Lcom/google/gson/f;->a:Ljava/util/ArrayList;

    invoke-virtual {p1}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object p1

    :goto_0
    invoke-interface {p1}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_5

    invoke-interface {p1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcom/google/gson/h;

    invoke-static {p0, v0}, Le/o$t;->d(Lj/c;Lcom/google/gson/h;)V

    goto :goto_0

    :cond_5
    invoke-virtual {p0}, Lj/c;->e()V

    goto/16 :goto_3

    :cond_6
    new-instance p0, Ljava/lang/IllegalStateException;

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "Not a JSON Array: "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p0

    :cond_7
    instance-of v0, p1, Lcom/google/gson/k;

    if-eqz v0, :cond_a

    invoke-virtual {p0}, Lj/c;->c()V

    if-eqz v0, :cond_9

    check-cast p1, Lcom/google/gson/k;

    iget-object p1, p1, Lcom/google/gson/k;->a:Ld/m;

    invoke-virtual {p1}, Ld/m;->entrySet()Ljava/util/Set;

    move-result-object p1

    check-cast p1, Ld/m$b;

    invoke-virtual {p1}, Ld/m$b;->iterator()Ljava/util/Iterator;

    move-result-object p1

    :goto_1
    move-object v0, p1

    check-cast v0, Ld/m$d;

    invoke-virtual {v0}, Ld/m$d;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_8

    move-object v0, p1

    check-cast v0, Ld/m$b$a;

    invoke-virtual {v0}, Ld/m$d;->a()Ld/m$e;

    move-result-object v0

    invoke-interface {v0}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/lang/String;

    invoke-virtual {p0, v1}, Lj/c;->g(Ljava/lang/String;)V

    invoke-interface {v0}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcom/google/gson/h;

    invoke-static {p0, v0}, Le/o$t;->d(Lj/c;Lcom/google/gson/h;)V

    goto :goto_1

    :cond_8
    invoke-virtual {p0}, Lj/c;->f()V

    goto :goto_3

    :cond_9
    new-instance p0, Ljava/lang/IllegalStateException;

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "Not a JSON Object: "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p0

    :cond_a
    new-instance p0, Ljava/lang/IllegalArgumentException;

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "Couldn\'t write "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object p1

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    throw p0

    :cond_b
    :goto_2
    invoke-virtual {p0}, Lj/c;->i()Lj/c;

    :goto_3
    return-void
.end method


# virtual methods
.method public final a(Lj/a;)Ljava/lang/Object;
    .locals 8
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    instance-of p0, p1, Le/e;

    if-eqz p0, :cond_1

    check-cast p1, Le/e;

    invoke-virtual {p1}, Le/e;->x()Lj/b;

    move-result-object p0

    sget-object v0, Lj/b;->e:Lj/b;

    if-eq p0, v0, :cond_0

    sget-object v0, Lj/b;->b:Lj/b;

    if-eq p0, v0, :cond_0

    sget-object v0, Lj/b;->d:Lj/b;

    if-eq p0, v0, :cond_0

    sget-object v0, Lj/b;->j:Lj/b;

    if-eq p0, v0, :cond_0

    invoke-virtual {p1}, Le/e;->J()Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Lcom/google/gson/h;

    invoke-virtual {p1}, Le/e;->D()V

    goto/16 :goto_9

    :cond_0
    new-instance p1, Ljava/lang/IllegalStateException;

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "Unexpected "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string p0, " when reading a JsonElement."

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-direct {p1, p0}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    :cond_1
    invoke-virtual {p1}, Lj/a;->x()Lj/b;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/Enum;->ordinal()I

    move-result v0

    const/4 v1, 0x2

    const/4 v2, 0x0

    if-eqz v0, :cond_3

    if-eq v0, v1, :cond_2

    move-object v0, v2

    goto :goto_0

    :cond_2
    invoke-virtual {p1}, Lj/a;->b()V

    new-instance v0, Lcom/google/gson/k;

    invoke-direct {v0}, Lcom/google/gson/k;-><init>()V

    goto :goto_0

    :cond_3
    invoke-virtual {p1}, Lj/a;->a()V

    new-instance v0, Lcom/google/gson/f;

    invoke-direct {v0}, Lcom/google/gson/f;-><init>()V

    :goto_0
    if-nez v0, :cond_4

    invoke-static {p1, p0}, Le/o$t;->c(Lj/a;Lj/b;)Lcom/google/gson/h;

    move-result-object p0

    goto/16 :goto_9

    :cond_4
    new-instance p0, Ljava/util/ArrayDeque;

    invoke-direct {p0}, Ljava/util/ArrayDeque;-><init>()V

    :cond_5
    :goto_1
    invoke-virtual {p1}, Lj/a;->k()Z

    move-result v3

    if-eqz v3, :cond_e

    instance-of v3, v0, Lcom/google/gson/k;

    if-eqz v3, :cond_6

    invoke-virtual {p1}, Lj/a;->r()Ljava/lang/String;

    move-result-object v3

    goto :goto_2

    :cond_6
    move-object v3, v2

    :goto_2
    invoke-virtual {p1}, Lj/a;->x()Lj/b;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Enum;->ordinal()I

    move-result v5

    if-eqz v5, :cond_8

    if-eq v5, v1, :cond_7

    move-object v5, v2

    goto :goto_3

    :cond_7
    invoke-virtual {p1}, Lj/a;->b()V

    new-instance v5, Lcom/google/gson/k;

    invoke-direct {v5}, Lcom/google/gson/k;-><init>()V

    goto :goto_3

    :cond_8
    invoke-virtual {p1}, Lj/a;->a()V

    new-instance v5, Lcom/google/gson/f;

    invoke-direct {v5}, Lcom/google/gson/f;-><init>()V

    :goto_3
    if-eqz v5, :cond_9

    const/4 v6, 0x1

    goto :goto_4

    :cond_9
    const/4 v6, 0x0

    :goto_4
    if-nez v5, :cond_a

    invoke-static {p1, v4}, Le/o$t;->c(Lj/a;Lj/b;)Lcom/google/gson/h;

    move-result-object v5

    :cond_a
    instance-of v4, v0, Lcom/google/gson/f;

    if-eqz v4, :cond_c

    move-object v3, v0

    check-cast v3, Lcom/google/gson/f;

    if-nez v5, :cond_b

    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    sget-object v4, Lcom/google/gson/j;->a:Lcom/google/gson/j;

    goto :goto_5

    :cond_b
    move-object v4, v5

    :goto_5
    iget-object v3, v3, Lcom/google/gson/f;->a:Ljava/util/ArrayList;

    invoke-virtual {v3, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_7

    :cond_c
    move-object v4, v0

    check-cast v4, Lcom/google/gson/k;

    if-nez v5, :cond_d

    invoke-virtual {v4}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    sget-object v7, Lcom/google/gson/j;->a:Lcom/google/gson/j;

    goto :goto_6

    :cond_d
    move-object v7, v5

    :goto_6
    iget-object v4, v4, Lcom/google/gson/k;->a:Ld/m;

    invoke-virtual {v4, v3, v7}, Ld/m;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :goto_7
    if-eqz v6, :cond_5

    invoke-virtual {p0, v0}, Ljava/util/ArrayDeque;->addLast(Ljava/lang/Object;)V

    move-object v0, v5

    goto :goto_1

    :cond_e
    instance-of v3, v0, Lcom/google/gson/f;

    if-eqz v3, :cond_f

    invoke-virtual {p1}, Lj/a;->e()V

    goto :goto_8

    :cond_f
    invoke-virtual {p1}, Lj/a;->f()V

    :goto_8
    invoke-virtual {p0}, Ljava/util/ArrayDeque;->isEmpty()Z

    move-result v3

    if-eqz v3, :cond_10

    move-object p0, v0

    :goto_9
    return-object p0

    :cond_10
    invoke-virtual {p0}, Ljava/util/ArrayDeque;->removeLast()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcom/google/gson/h;

    goto/16 :goto_1
.end method

.method public final bridge synthetic b(Lj/c;Ljava/lang/Object;)V
    .locals 0
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    check-cast p2, Lcom/google/gson/h;

    invoke-static {p1, p2}, Le/o$t;->d(Lj/c;Lcom/google/gson/h;)V

    return-void
.end method
