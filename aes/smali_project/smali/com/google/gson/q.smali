.class public abstract enum Lcom/google/gson/q;
.super Ljava/lang/Enum;
.source "SourceFile"

# interfaces
.implements Lcom/google/gson/r;


# annotations
.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum<",
        "Lcom/google/gson/q;",
        ">;",
        "Lcom/google/gson/r;"
    }
.end annotation


# static fields
.field public static final enum a:Lcom/google/gson/q$a;

.field public static final enum b:Lcom/google/gson/q$b;

.field public static final synthetic c:[Lcom/google/gson/q;


# direct methods
.method static constructor <clinit>()V
    .locals 6

    new-instance v0, Lcom/google/gson/q$a;

    invoke-direct {v0}, Lcom/google/gson/q$a;-><init>()V

    sput-object v0, Lcom/google/gson/q;->a:Lcom/google/gson/q$a;

    new-instance v1, Lcom/google/gson/q$b;

    invoke-direct {v1}, Lcom/google/gson/q$b;-><init>()V

    sput-object v1, Lcom/google/gson/q;->b:Lcom/google/gson/q$b;

    new-instance v2, Lcom/google/gson/q$c;

    invoke-direct {v2}, Lcom/google/gson/q$c;-><init>()V

    new-instance v3, Lcom/google/gson/q$d;

    invoke-direct {v3}, Lcom/google/gson/q$d;-><init>()V

    const/4 v4, 0x4

    new-array v4, v4, [Lcom/google/gson/q;

    const/4 v5, 0x0

    aput-object v0, v4, v5

    const/4 v0, 0x1

    aput-object v1, v4, v0

    const/4 v0, 0x2

    aput-object v2, v4, v0

    const/4 v0, 0x3

    aput-object v3, v4, v0

    sput-object v4, Lcom/google/gson/q;->c:[Lcom/google/gson/q;

    return-void
.end method

.method public constructor <init>()V
    .locals 0

    const/4 p0, 0x0

    throw p0
.end method

.method public static valueOf(Ljava/lang/String;)Lcom/google/gson/q;
    .locals 1

    const-class v0, Lcom/google/gson/q;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object p0

    check-cast p0, Lcom/google/gson/q;

    return-object p0
.end method

.method public static values()[Lcom/google/gson/q;
    .locals 1

    sget-object v0, Lcom/google/gson/q;->c:[Lcom/google/gson/q;

    invoke-virtual {v0}, [Lcom/google/gson/q;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Lcom/google/gson/q;

    return-object v0
.end method
