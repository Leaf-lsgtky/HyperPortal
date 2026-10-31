.class public final Ld/i$a;
.super Lcom/google/gson/s;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Ld/i;->a(Lcom/google/gson/d;Li/a;)Lcom/google/gson/s;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = null
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Lcom/google/gson/s<",
        "TT;>;"
    }
.end annotation


# instance fields
.field public a:Lcom/google/gson/s;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Lcom/google/gson/s<",
            "TT;>;"
        }
    .end annotation
.end field

.field public final synthetic b:Z

.field public final synthetic c:Z

.field public final synthetic d:Lcom/google/gson/d;

.field public final synthetic e:Li/a;

.field public final synthetic f:Ld/i;


# direct methods
.method public constructor <init>(Ld/i;ZZLcom/google/gson/d;Li/a;)V
    .locals 0

    iput-object p1, p0, Ld/i$a;->f:Ld/i;

    iput-boolean p2, p0, Ld/i$a;->b:Z

    iput-boolean p3, p0, Ld/i$a;->c:Z

    iput-object p4, p0, Ld/i$a;->d:Lcom/google/gson/d;

    iput-object p5, p0, Ld/i$a;->e:Li/a;

    invoke-direct {p0}, Lcom/google/gson/s;-><init>()V

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

    iget-boolean v0, p0, Ld/i$a;->b:Z

    if-eqz v0, :cond_0

    invoke-virtual {p1}, Lj/a;->D()V

    const/4 p0, 0x0

    return-object p0

    :cond_0
    iget-object v0, p0, Ld/i$a;->a:Lcom/google/gson/s;

    if-eqz v0, :cond_1

    goto :goto_0

    :cond_1
    iget-object v0, p0, Ld/i$a;->e:Li/a;

    iget-object v1, p0, Ld/i$a;->d:Lcom/google/gson/d;

    iget-object v2, p0, Ld/i$a;->f:Ld/i;

    invoke-virtual {v1, v2, v0}, Lcom/google/gson/d;->d(Lcom/google/gson/t;Li/a;)Lcom/google/gson/s;

    move-result-object v0

    iput-object v0, p0, Ld/i$a;->a:Lcom/google/gson/s;

    :goto_0
    invoke-virtual {v0, p1}, Lcom/google/gson/s;->a(Lj/a;)Ljava/lang/Object;

    move-result-object p0

    return-object p0
.end method

.method public final b(Lj/c;Ljava/lang/Object;)V
    .locals 3
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

    iget-boolean v0, p0, Ld/i$a;->c:Z

    if-eqz v0, :cond_0

    invoke-virtual {p1}, Lj/c;->i()Lj/c;

    return-void

    :cond_0
    iget-object v0, p0, Ld/i$a;->a:Lcom/google/gson/s;

    if-eqz v0, :cond_1

    goto :goto_0

    :cond_1
    iget-object v0, p0, Ld/i$a;->e:Li/a;

    iget-object v1, p0, Ld/i$a;->d:Lcom/google/gson/d;

    iget-object v2, p0, Ld/i$a;->f:Ld/i;

    invoke-virtual {v1, v2, v0}, Lcom/google/gson/d;->d(Lcom/google/gson/t;Li/a;)Lcom/google/gson/s;

    move-result-object v0

    iput-object v0, p0, Ld/i$a;->a:Lcom/google/gson/s;

    :goto_0
    invoke-virtual {v0, p1, p2}, Lcom/google/gson/s;->b(Lj/c;Ljava/lang/Object;)V

    return-void
.end method
