.class public final Le/m;
.super Le/l;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Le/m$a;
    }
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "<T:",
        "Ljava/lang/Object;",
        ">",
        "Le/l<",
        "TT;>;"
    }
.end annotation


# instance fields
.field public final a:Lcom/google/gson/n;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Lcom/google/gson/n<",
            "TT;>;"
        }
    .end annotation
.end field

.field public final b:Lcom/google/gson/g;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Lcom/google/gson/g<",
            "TT;>;"
        }
    .end annotation
.end field

.field public final c:Lcom/google/gson/d;

.field public final d:Li/a;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Li/a<",
            "TT;>;"
        }
    .end annotation
.end field

.field public final e:Z

.field public volatile f:Lcom/google/gson/s;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Lcom/google/gson/s<",
            "TT;>;"
        }
    .end annotation
.end field


# direct methods
.method public constructor <init>(Lcom/google/gson/n;Lcom/google/gson/g;Lcom/google/gson/d;Li/a;Z)V
    .locals 0

    invoke-direct {p0}, Le/l;-><init>()V

    iput-object p1, p0, Le/m;->a:Lcom/google/gson/n;

    iput-object p2, p0, Le/m;->b:Lcom/google/gson/g;

    iput-object p3, p0, Le/m;->c:Lcom/google/gson/d;

    iput-object p4, p0, Le/m;->d:Li/a;

    iput-boolean p5, p0, Le/m;->e:Z

    return-void
.end method


# virtual methods
.method public final a(Lj/a;)Ljava/lang/Object;
    .locals 3
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lj/a;",
            ")TT;"
        }
    .end annotation

    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    iget-object v0, p0, Le/m;->b:Lcom/google/gson/g;

    if-nez v0, :cond_0

    invoke-virtual {p0}, Le/m;->d()Lcom/google/gson/s;

    move-result-object p0

    invoke-virtual {p0, p1}, Lcom/google/gson/s;->a(Lj/a;)Ljava/lang/Object;

    move-result-object p0

    return-object p0

    :cond_0
    :try_start_0
    invoke-virtual {p1}, Lj/a;->x()Lj/b;
    :try_end_0
    .catch Ljava/io/EOFException; {:try_start_0 .. :try_end_0} :catch_4
    .catch Lj/d; {:try_start_0 .. :try_end_0} :catch_2
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/NumberFormatException; {:try_start_0 .. :try_end_0} :catch_0

    const/4 v1, 0x0

    :try_start_1
    sget-object v2, Le/o;->z:Le/o$t;

    invoke-virtual {v2, p1}, Le/o$t;->a(Lj/a;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Lcom/google/gson/h;
    :try_end_1
    .catch Ljava/io/EOFException; {:try_start_1 .. :try_end_1} :catch_3
    .catch Lj/d; {:try_start_1 .. :try_end_1} :catch_2
    .catch Ljava/io/IOException; {:try_start_1 .. :try_end_1} :catch_1
    .catch Ljava/lang/NumberFormatException; {:try_start_1 .. :try_end_1} :catch_0

    goto :goto_4

    :catch_0
    move-exception p0

    goto :goto_0

    :catch_1
    move-exception p0

    goto :goto_1

    :catch_2
    move-exception p0

    goto :goto_2

    :catch_3
    move-exception p1

    goto :goto_3

    :goto_0
    new-instance p1, Lcom/google/gson/o;

    invoke-direct {p1, p0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/Throwable;)V

    throw p1

    :goto_1
    new-instance p1, Lcom/google/gson/i;

    invoke-direct {p1, p0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/Throwable;)V

    throw p1

    :goto_2
    new-instance p1, Lcom/google/gson/o;

    invoke-direct {p1, p0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/Throwable;)V

    throw p1

    :catch_4
    move-exception p1

    const/4 v1, 0x1

    :goto_3
    if-eqz v1, :cond_2

    sget-object p1, Lcom/google/gson/j;->a:Lcom/google/gson/j;

    :goto_4
    iget-boolean v1, p0, Le/m;->e:Z

    if-eqz v1, :cond_1

    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    instance-of p1, p1, Lcom/google/gson/j;

    if-eqz p1, :cond_1

    const/4 p0, 0x0

    return-object p0

    :cond_1
    iget-object p0, p0, Le/m;->d:Li/a;

    iget-object p0, p0, Li/a;->b:Ljava/lang/reflect/Type;

    invoke-interface {v0}, Lcom/google/gson/g;->a()Ljava/lang/Object;

    move-result-object p0

    return-object p0

    :cond_2
    new-instance p0, Lcom/google/gson/o;

    invoke-direct {p0, p1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/Throwable;)V

    throw p0
.end method

.method public final b(Lj/c;Ljava/lang/Object;)V
    .locals 2
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lj/c;",
            "TT;)V"
        }
    .end annotation

    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    iget-object v0, p0, Le/m;->a:Lcom/google/gson/n;

    if-nez v0, :cond_0

    invoke-virtual {p0}, Le/m;->d()Lcom/google/gson/s;

    move-result-object p0

    invoke-virtual {p0, p1, p2}, Lcom/google/gson/s;->b(Lj/c;Ljava/lang/Object;)V

    return-void

    :cond_0
    iget-boolean v1, p0, Le/m;->e:Z

    if-eqz v1, :cond_1

    if-nez p2, :cond_1

    invoke-virtual {p1}, Lj/c;->i()Lj/c;

    return-void

    :cond_1
    iget-object p0, p0, Le/m;->d:Li/a;

    iget-object p0, p0, Li/a;->b:Ljava/lang/reflect/Type;

    invoke-interface {v0}, Lcom/google/gson/n;->a()Lcom/google/gson/h;

    move-result-object p0

    sget-object p2, Le/o;->z:Le/o$t;

    invoke-virtual {p2}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    invoke-static {p1, p0}, Le/o$t;->d(Lj/c;Lcom/google/gson/h;)V

    return-void
.end method

.method public final c()Lcom/google/gson/s;
    .locals 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Lcom/google/gson/s<",
            "TT;>;"
        }
    .end annotation

    iget-object v0, p0, Le/m;->a:Lcom/google/gson/n;

    if-eqz v0, :cond_0

    goto :goto_0

    :cond_0
    invoke-virtual {p0}, Le/m;->d()Lcom/google/gson/s;

    move-result-object p0

    :goto_0
    return-object p0
.end method

.method public final d()Lcom/google/gson/s;
    .locals 3
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Lcom/google/gson/s<",
            "TT;>;"
        }
    .end annotation

    iget-object v0, p0, Le/m;->f:Lcom/google/gson/s;

    if-eqz v0, :cond_0

    goto :goto_0

    :cond_0
    iget-object v0, p0, Le/m;->c:Lcom/google/gson/d;

    iget-object v1, p0, Le/m;->d:Li/a;

    const/4 v2, 0x0

    invoke-virtual {v0, v2, v1}, Lcom/google/gson/d;->d(Lcom/google/gson/t;Li/a;)Lcom/google/gson/s;

    move-result-object v0

    iput-object v0, p0, Le/m;->f:Lcom/google/gson/s;

    :goto_0
    return-object v0
.end method
