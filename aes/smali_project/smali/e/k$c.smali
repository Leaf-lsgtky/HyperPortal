.class public final Le/k$c;
.super Le/k$a;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Le/k;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x19
    name = "c"
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "<T:",
        "Ljava/lang/Object;",
        ">",
        "Le/k$a<",
        "TT;TT;>;"
    }
.end annotation


# instance fields
.field public final b:Ld/n;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ld/n<",
            "TT;>;"
        }
    .end annotation
.end field


# direct methods
.method public constructor <init>(Ld/n;Ljava/util/LinkedHashMap;)V
    .locals 0

    invoke-direct {p0, p2}, Le/k$a;-><init>(Ljava/util/LinkedHashMap;)V

    iput-object p1, p0, Le/k$c;->b:Ld/n;

    return-void
.end method


# virtual methods
.method public final c()Ljava/lang/Object;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()TT;"
        }
    .end annotation

    iget-object p0, p0, Le/k$c;->b:Ld/n;

    invoke-interface {p0}, Ld/n;->a()Ljava/lang/Object;

    move-result-object p0

    return-object p0
.end method

.method public final d(Ljava/lang/Object;)Ljava/lang/Object;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(TT;)TT;"
        }
    .end annotation

    return-object p1
.end method

.method public final e(Ljava/lang/Object;Lj/a;Le/k$b;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(TT;",
            "Lj/a;",
            "Le/k$b;",
            ")V"
        }
    .end annotation

    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/IllegalAccessException;,
            Ljava/io/IOException;
        }
    .end annotation

    invoke-virtual {p3, p2, p1}, Le/k$b;->b(Lj/a;Ljava/lang/Object;)V

    return-void
.end method
