.class public final Le/f$a;
.super Lcom/google/gson/s;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Le/f;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x11
    name = "a"
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "<K:",
        "Ljava/lang/Object;",
        "V:",
        "Ljava/lang/Object;",
        ">",
        "Lcom/google/gson/s<",
        "Ljava/util/Map<",
        "TK;TV;>;>;"
    }
.end annotation


# instance fields
.field public final a:Le/n;

.field public final b:Le/n;

.field public final c:Ld/n;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ld/n<",
            "+",
            "Ljava/util/Map<",
            "TK;TV;>;>;"
        }
    .end annotation
.end field

.field public final synthetic d:Le/f;


# direct methods
.method public constructor <init>(Le/f;Lcom/google/gson/d;Ljava/lang/reflect/Type;Lcom/google/gson/s;Ljava/lang/reflect/Type;Lcom/google/gson/s;Ld/n;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lcom/google/gson/d;",
            "Ljava/lang/reflect/Type;",
            "Lcom/google/gson/s<",
            "TK;>;",
            "Ljava/lang/reflect/Type;",
            "Lcom/google/gson/s<",
            "TV;>;",
            "Ld/n<",
            "+",
            "Ljava/util/Map<",
            "TK;TV;>;>;)V"
        }
    .end annotation

    iput-object p1, p0, Le/f$a;->d:Le/f;

    invoke-direct {p0}, Lcom/google/gson/s;-><init>()V

    new-instance p1, Le/n;

    invoke-direct {p1, p2, p4, p3}, Le/n;-><init>(Lcom/google/gson/d;Lcom/google/gson/s;Ljava/lang/reflect/Type;)V

    iput-object p1, p0, Le/f$a;->a:Le/n;

    new-instance p1, Le/n;

    invoke-direct {p1, p2, p6, p5}, Le/n;-><init>(Lcom/google/gson/d;Lcom/google/gson/s;Ljava/lang/reflect/Type;)V

    iput-object p1, p0, Le/f$a;->b:Le/n;

    iput-object p7, p0, Le/f$a;->c:Ld/n;

    return-void
.end method


# virtual methods
.method public final a(Lj/a;)Ljava/lang/Object;
    .locals 5
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    invoke-virtual {p1}, Lj/a;->x()Lj/b;

    move-result-object v0

    sget-object v1, Lj/b;->i:Lj/b;

    if-ne v0, v1, :cond_0

    invoke-virtual {p1}, Lj/a;->t()V

    const/4 p0, 0x0

    goto/16 :goto_3

    :cond_0
    iget-object v1, p0, Le/f$a;->c:Ld/n;

    invoke-interface {v1}, Ld/n;->a()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/util/Map;

    sget-object v2, Lj/b;->a:Lj/b;

    iget-object v3, p0, Le/f$a;->b:Le/n;

    iget-object p0, p0, Le/f$a;->a:Le/n;

    const-string v4, "duplicate key: "

    if-ne v0, v2, :cond_3

    invoke-virtual {p1}, Lj/a;->a()V

    :goto_0
    invoke-virtual {p1}, Lj/a;->k()Z

    move-result v0

    if-eqz v0, :cond_2

    invoke-virtual {p1}, Lj/a;->a()V

    iget-object v0, p0, Le/n;->b:Lcom/google/gson/s;

    invoke-virtual {v0, p1}, Lcom/google/gson/s;->a(Lj/a;)Ljava/lang/Object;

    move-result-object v0

    iget-object v2, v3, Le/n;->b:Lcom/google/gson/s;

    invoke-virtual {v2, p1}, Lcom/google/gson/s;->a(Lj/a;)Ljava/lang/Object;

    move-result-object v2

    invoke-interface {v1, v0, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    if-nez v2, :cond_1

    invoke-virtual {p1}, Lj/a;->e()V

    goto :goto_0

    :cond_1
    new-instance p0, Lcom/google/gson/o;

    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw p0

    :cond_2
    invoke-virtual {p1}, Lj/a;->e()V

    goto :goto_2

    :cond_3
    invoke-virtual {p1}, Lj/a;->b()V

    :goto_1
    invoke-virtual {p1}, Lj/a;->k()Z

    move-result v0

    if-eqz v0, :cond_5

    sget-object v0, Ld/k;->a:Lj/a$a;

    invoke-virtual {v0, p1}, Lj/a$a;->a(Lj/a;)V

    iget-object v0, p0, Le/n;->b:Lcom/google/gson/s;

    invoke-virtual {v0, p1}, Lcom/google/gson/s;->a(Lj/a;)Ljava/lang/Object;

    move-result-object v0

    iget-object v2, v3, Le/n;->b:Lcom/google/gson/s;

    invoke-virtual {v2, p1}, Lcom/google/gson/s;->a(Lj/a;)Ljava/lang/Object;

    move-result-object v2

    invoke-interface {v1, v0, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    if-nez v2, :cond_4

    goto :goto_1

    :cond_4
    new-instance p0, Lcom/google/gson/o;

    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw p0

    :cond_5
    invoke-virtual {p1}, Lj/a;->f()V

    :goto_2
    move-object p0, v1

    :goto_3
    return-object p0
.end method

.method public final b(Lj/c;Ljava/lang/Object;)V
    .locals 2
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    check-cast p2, Ljava/util/Map;

    if-nez p2, :cond_0

    invoke-virtual {p1}, Lj/c;->i()Lj/c;

    goto :goto_1

    :cond_0
    iget-object v0, p0, Le/f$a;->d:Le/f;

    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    invoke-virtual {p1}, Lj/c;->c()V

    invoke-interface {p2}, Ljava/util/Map;->entrySet()Ljava/util/Set;

    move-result-object p2

    invoke-interface {p2}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object p2

    :goto_0
    invoke-interface {p2}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_1

    invoke-interface {p2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/util/Map$Entry;

    invoke-interface {v0}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v1

    invoke-static {v1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {p1, v1}, Lj/c;->g(Ljava/lang/String;)V

    invoke-interface {v0}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v0

    iget-object v1, p0, Le/f$a;->b:Le/n;

    invoke-virtual {v1, p1, v0}, Le/n;->b(Lj/c;Ljava/lang/Object;)V

    goto :goto_0

    :cond_1
    invoke-virtual {p1}, Lj/c;->f()V

    :goto_1
    return-void
.end method
