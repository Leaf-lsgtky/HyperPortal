.class public final Le/i;
.super Lcom/google/gson/s;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/Signature;
    value = {
        "Lcom/google/gson/s<",
        "Ljava/lang/Object;",
        ">;"
    }
.end annotation


# static fields
.field public static final c:Le/h;


# instance fields
.field public final a:Lcom/google/gson/d;

.field public final b:Lcom/google/gson/q;


# direct methods
.method static constructor <clinit>()V
    .locals 1

    new-instance v0, Le/h;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    sput-object v0, Le/i;->c:Le/h;

    return-void
.end method

.method public constructor <init>(Lcom/google/gson/d;)V
    .locals 1

    sget-object v0, Lcom/google/gson/q;->a:Lcom/google/gson/q$a;

    invoke-direct {p0}, Lcom/google/gson/s;-><init>()V

    iput-object p1, p0, Le/i;->a:Lcom/google/gson/d;

    iput-object v0, p0, Le/i;->b:Lcom/google/gson/q;

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

    invoke-virtual {p1}, Lj/a;->x()Lj/b;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Enum;->ordinal()I

    move-result v1

    const/4 v2, 0x2

    const/4 v3, 0x0

    if-eqz v1, :cond_1

    if-eq v1, v2, :cond_0

    move-object v1, v3

    goto :goto_0

    :cond_0
    invoke-virtual {p1}, Lj/a;->b()V

    new-instance v1, Ld/m;

    invoke-direct {v1}, Ld/m;-><init>()V

    goto :goto_0

    :cond_1
    invoke-virtual {p1}, Lj/a;->a()V

    new-instance v1, Ljava/util/ArrayList;

    invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V

    :goto_0
    if-nez v1, :cond_2

    invoke-virtual {p0, p1, v0}, Le/i;->c(Lj/a;Lj/b;)Ljava/io/Serializable;

    move-result-object p0

    return-object p0

    :cond_2
    new-instance v0, Ljava/util/ArrayDeque;

    invoke-direct {v0}, Ljava/util/ArrayDeque;-><init>()V

    :cond_3
    :goto_1
    invoke-virtual {p1}, Lj/a;->k()Z

    move-result v4

    if-eqz v4, :cond_a

    instance-of v4, v1, Ljava/util/Map;

    if-eqz v4, :cond_4

    invoke-virtual {p1}, Lj/a;->r()Ljava/lang/String;

    move-result-object v4

    goto :goto_2

    :cond_4
    move-object v4, v3

    :goto_2
    invoke-virtual {p1}, Lj/a;->x()Lj/b;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/Enum;->ordinal()I

    move-result v6

    if-eqz v6, :cond_6

    if-eq v6, v2, :cond_5

    move-object v6, v3

    goto :goto_3

    :cond_5
    invoke-virtual {p1}, Lj/a;->b()V

    new-instance v6, Ld/m;

    invoke-direct {v6}, Ld/m;-><init>()V

    goto :goto_3

    :cond_6
    invoke-virtual {p1}, Lj/a;->a()V

    new-instance v6, Ljava/util/ArrayList;

    invoke-direct {v6}, Ljava/util/ArrayList;-><init>()V

    :goto_3
    if-eqz v6, :cond_7

    const/4 v7, 0x1

    goto :goto_4

    :cond_7
    const/4 v7, 0x0

    :goto_4
    if-nez v6, :cond_8

    invoke-virtual {p0, p1, v5}, Le/i;->c(Lj/a;Lj/b;)Ljava/io/Serializable;

    move-result-object v6

    :cond_8
    instance-of v5, v1, Ljava/util/List;

    if-eqz v5, :cond_9

    move-object v4, v1

    check-cast v4, Ljava/util/List;

    invoke-interface {v4, v6}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    goto :goto_5

    :cond_9
    move-object v5, v1

    check-cast v5, Ljava/util/Map;

    invoke-interface {v5, v4, v6}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :goto_5
    if-eqz v7, :cond_3

    invoke-virtual {v0, v1}, Ljava/util/ArrayDeque;->addLast(Ljava/lang/Object;)V

    move-object v1, v6

    goto :goto_1

    :cond_a
    instance-of v4, v1, Ljava/util/List;

    if-eqz v4, :cond_b

    invoke-virtual {p1}, Lj/a;->e()V

    goto :goto_6

    :cond_b
    invoke-virtual {p1}, Lj/a;->f()V

    :goto_6
    invoke-virtual {v0}, Ljava/util/ArrayDeque;->isEmpty()Z

    move-result v4

    if-eqz v4, :cond_c

    return-object v1

    :cond_c
    invoke-virtual {v0}, Ljava/util/ArrayDeque;->removeLast()Ljava/lang/Object;

    move-result-object v1

    goto :goto_1
.end method

.method public final b(Lj/c;Ljava/lang/Object;)V
    .locals 2
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    if-nez p2, :cond_0

    invoke-virtual {p1}, Lj/c;->i()Lj/c;

    return-void

    :cond_0
    invoke-virtual {p2}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v0

    iget-object p0, p0, Le/i;->a:Lcom/google/gson/d;

    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    new-instance v1, Li/a;

    invoke-direct {v1, v0}, Li/a;-><init>(Ljava/lang/reflect/Type;)V

    invoke-virtual {p0, v1}, Lcom/google/gson/d;->c(Li/a;)Lcom/google/gson/s;

    move-result-object p0

    instance-of v0, p0, Le/i;

    if-eqz v0, :cond_1

    invoke-virtual {p1}, Lj/c;->c()V

    invoke-virtual {p1}, Lj/c;->f()V

    return-void

    :cond_1
    invoke-virtual {p0, p1, p2}, Lcom/google/gson/s;->b(Lj/c;Ljava/lang/Object;)V

    return-void
.end method

.method public final c(Lj/a;Lj/b;)Ljava/io/Serializable;
    .locals 2
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    invoke-virtual {p2}, Ljava/lang/Enum;->ordinal()I

    move-result v0

    const/4 v1, 0x5

    if-eq v0, v1, :cond_3

    const/4 v1, 0x6

    if-eq v0, v1, :cond_2

    const/4 p0, 0x7

    if-eq v0, p0, :cond_1

    const/16 p0, 0x8

    if-ne v0, p0, :cond_0

    invoke-virtual {p1}, Lj/a;->t()V

    const/4 p0, 0x0

    return-object p0

    :cond_0
    new-instance p0, Ljava/lang/IllegalStateException;

    new-instance p1, Ljava/lang/StringBuilder;

    const-string v0, "Unexpected token: "

    invoke-direct {p1, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p0

    :cond_1
    invoke-virtual {p1}, Lj/a;->n()Z

    move-result p0

    invoke-static {p0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object p0

    return-object p0

    :cond_2
    iget-object p0, p0, Le/i;->b:Lcom/google/gson/q;

    invoke-interface {p0, p1}, Lcom/google/gson/r;->a(Lj/a;)Ljava/lang/Number;

    move-result-object p0

    return-object p0

    :cond_3
    invoke-virtual {p1}, Lj/a;->v()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method
