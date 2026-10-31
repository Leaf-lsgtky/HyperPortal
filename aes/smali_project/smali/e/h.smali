.class public final Le/h;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lcom/google/gson/t;


# virtual methods
.method public final a(Lcom/google/gson/d;Li/a;)Lcom/google/gson/s;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "<T:",
            "Ljava/lang/Object;",
            ">(",
            "Lcom/google/gson/d;",
            "Li/a<",
            "TT;>;)",
            "Lcom/google/gson/s<",
            "TT;>;"
        }
    .end annotation

    iget-object p0, p2, Li/a;->a:Ljava/lang/Class;

    const-class p2, Ljava/lang/Object;

    if-ne p0, p2, :cond_0

    new-instance p0, Le/i;

    invoke-direct {p0, p1}, Le/i;-><init>(Lcom/google/gson/d;)V

    return-object p0

    :cond_0
    const/4 p0, 0x0

    return-object p0
.end method
