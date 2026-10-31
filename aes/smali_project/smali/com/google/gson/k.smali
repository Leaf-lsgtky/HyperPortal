.class public final Lcom/google/gson/k;
.super Lcom/google/gson/h;
.source "SourceFile"


# instance fields
.field public final a:Ld/m;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ld/m<",
            "Ljava/lang/String;",
            "Lcom/google/gson/h;",
            ">;"
        }
    .end annotation
.end field


# direct methods
.method public constructor <init>()V
    .locals 2

    invoke-direct {p0}, Lcom/google/gson/h;-><init>()V

    new-instance v0, Ld/m;

    const/4 v1, 0x0

    invoke-direct {v0, v1}, Ld/m;-><init>(Z)V

    iput-object v0, p0, Lcom/google/gson/k;->a:Ld/m;

    return-void
.end method


# virtual methods
.method public final equals(Ljava/lang/Object;)Z
    .locals 1

    if-eq p1, p0, :cond_1

    instance-of v0, p1, Lcom/google/gson/k;

    if-eqz v0, :cond_0

    check-cast p1, Lcom/google/gson/k;

    iget-object p1, p1, Lcom/google/gson/k;->a:Ld/m;

    iget-object p0, p0, Lcom/google/gson/k;->a:Ld/m;

    invoke-virtual {p1, p0}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_0

    goto :goto_0

    :cond_0
    const/4 p0, 0x0

    goto :goto_1

    :cond_1
    :goto_0
    const/4 p0, 0x1

    :goto_1
    return p0
.end method

.method public final hashCode()I
    .locals 0

    iget-object p0, p0, Lcom/google/gson/k;->a:Ld/m;

    invoke-virtual {p0}, Ljava/lang/Object;->hashCode()I

    move-result p0

    return p0
.end method
