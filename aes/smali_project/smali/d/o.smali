.class public final Ld/o;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Ld/o$a;
    }
.end annotation


# direct methods
.method public static a(Ljava/util/List;Ljava/lang/Class;)Lcom/google/gson/p$a;
    .locals 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List<",
            "Lcom/google/gson/p;",
            ">;",
            "Ljava/lang/Class<",
            "*>;)",
            "Lcom/google/gson/p$a;"
        }
    .end annotation

    invoke-interface {p0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :cond_0
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result p1

    if-eqz p1, :cond_1

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Lcom/google/gson/p;

    invoke-interface {p1}, Lcom/google/gson/p;->a()Lcom/google/gson/p$a;

    move-result-object p1

    sget-object v0, Lcom/google/gson/p$a;->b:Lcom/google/gson/p$a;

    if-eq p1, v0, :cond_0

    return-object p1

    :cond_1
    sget-object p0, Lcom/google/gson/p$a;->a:Lcom/google/gson/p$a;

    return-object p0
.end method
