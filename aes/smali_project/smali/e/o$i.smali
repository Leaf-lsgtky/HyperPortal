.class public final Le/o$i;
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
        "Ld/l;",
        ">;"
    }
.end annotation


# virtual methods
.method public final a(Lj/a;)Ljava/lang/Object;
    .locals 1
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    invoke-virtual {p1}, Lj/a;->x()Lj/b;

    move-result-object p0

    sget-object v0, Lj/b;->i:Lj/b;

    if-ne p0, v0, :cond_0

    invoke-virtual {p1}, Lj/a;->t()V

    const/4 p0, 0x0

    goto :goto_0

    :cond_0
    new-instance p0, Ld/l;

    invoke-virtual {p1}, Lj/a;->v()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Ld/l;-><init>(Ljava/lang/String;)V

    :goto_0
    return-object p0
.end method

.method public final b(Lj/c;Ljava/lang/Object;)V
    .locals 0
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    check-cast p2, Ld/l;

    invoke-virtual {p1, p2}, Lj/c;->n(Ljava/lang/Number;)V

    return-void
.end method
