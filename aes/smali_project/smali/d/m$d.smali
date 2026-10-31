.class public abstract Ld/m$d;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/util/Iterator;


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Ld/m;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x401
    name = "d"
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "<T:",
        "Ljava/lang/Object;",
        ">",
        "Ljava/lang/Object;",
        "Ljava/util/Iterator<",
        "TT;>;"
    }
.end annotation


# instance fields
.field public a:Ld/m$e;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ld/m$e<",
            "TK;TV;>;"
        }
    .end annotation
.end field

.field public b:Ld/m$e;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ld/m$e<",
            "TK;TV;>;"
        }
    .end annotation
.end field

.field public c:I

.field public final synthetic d:Ld/m;


# direct methods
.method public constructor <init>(Ld/m;)V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Ld/m$d;->d:Ld/m;

    iget-object v0, p1, Ld/m;->f:Ld/m$e;

    iget-object v0, v0, Ld/m$e;->d:Ld/m$e;

    iput-object v0, p0, Ld/m$d;->a:Ld/m$e;

    const/4 v0, 0x0

    iput-object v0, p0, Ld/m$d;->b:Ld/m$e;

    iget p1, p1, Ld/m;->e:I

    iput p1, p0, Ld/m$d;->c:I

    return-void
.end method


# virtual methods
.method public final a()Ld/m$e;
    .locals 3
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ld/m$e<",
            "TK;TV;>;"
        }
    .end annotation

    iget-object v0, p0, Ld/m$d;->a:Ld/m$e;

    iget-object v1, p0, Ld/m$d;->d:Ld/m;

    iget-object v2, v1, Ld/m;->f:Ld/m$e;

    if-eq v0, v2, :cond_1

    iget v1, v1, Ld/m;->e:I

    iget v2, p0, Ld/m$d;->c:I

    if-ne v1, v2, :cond_0

    iget-object v1, v0, Ld/m$e;->d:Ld/m$e;

    iput-object v1, p0, Ld/m$d;->a:Ld/m$e;

    iput-object v0, p0, Ld/m$d;->b:Ld/m$e;

    return-object v0

    :cond_0
    new-instance p0, Ljava/util/ConcurrentModificationException;

    invoke-direct {p0}, Ljava/util/ConcurrentModificationException;-><init>()V

    throw p0

    :cond_1
    new-instance p0, Ljava/util/NoSuchElementException;

    invoke-direct {p0}, Ljava/util/NoSuchElementException;-><init>()V

    throw p0
.end method

.method public final hasNext()Z
    .locals 1

    iget-object v0, p0, Ld/m$d;->a:Ld/m$e;

    iget-object p0, p0, Ld/m$d;->d:Ld/m;

    iget-object p0, p0, Ld/m;->f:Ld/m$e;

    if-eq v0, p0, :cond_0

    const/4 p0, 0x1

    goto :goto_0

    :cond_0
    const/4 p0, 0x0

    :goto_0
    return p0
.end method

.method public next()Ljava/lang/Object;
    .locals 0

    invoke-virtual {p0}, Ld/m$d;->a()Ld/m$e;

    move-result-object p0

    return-object p0
.end method

.method public final remove()V
    .locals 3

    iget-object v0, p0, Ld/m$d;->b:Ld/m$e;

    if-eqz v0, :cond_0

    const/4 v1, 0x1

    iget-object v2, p0, Ld/m$d;->d:Ld/m;

    invoke-virtual {v2, v0, v1}, Ld/m;->c(Ld/m$e;Z)V

    const/4 v0, 0x0

    iput-object v0, p0, Ld/m$d;->b:Ld/m$e;

    iget v0, v2, Ld/m;->e:I

    iput v0, p0, Ld/m$d;->c:I

    return-void

    :cond_0
    new-instance p0, Ljava/lang/IllegalStateException;

    invoke-direct {p0}, Ljava/lang/IllegalStateException;-><init>()V

    throw p0
.end method
