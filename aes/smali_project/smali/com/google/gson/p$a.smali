.class public final enum Lcom/google/gson/p$a;
.super Ljava/lang/Enum;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/google/gson/p;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x4019
    name = "a"
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum<",
        "Lcom/google/gson/p$a;",
        ">;"
    }
.end annotation


# static fields
.field public static final enum a:Lcom/google/gson/p$a;

.field public static final enum b:Lcom/google/gson/p$a;

.field public static final enum c:Lcom/google/gson/p$a;

.field public static final enum d:Lcom/google/gson/p$a;

.field public static final synthetic e:[Lcom/google/gson/p$a;


# direct methods
.method static constructor <clinit>()V
    .locals 6

    new-instance v0, Lcom/google/gson/p$a;

    const-string v1, "ALLOW"

    const/4 v2, 0x0

    invoke-direct {v0, v1, v2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v0, Lcom/google/gson/p$a;->a:Lcom/google/gson/p$a;

    new-instance v1, Lcom/google/gson/p$a;

    const-string v2, "INDECISIVE"

    const/4 v3, 0x1

    invoke-direct {v1, v2, v3}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v1, Lcom/google/gson/p$a;->b:Lcom/google/gson/p$a;

    new-instance v2, Lcom/google/gson/p$a;

    const-string v3, "BLOCK_INACCESSIBLE"

    const/4 v4, 0x2

    invoke-direct {v2, v3, v4}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v2, Lcom/google/gson/p$a;->c:Lcom/google/gson/p$a;

    new-instance v3, Lcom/google/gson/p$a;

    const-string v4, "BLOCK_ALL"

    const/4 v5, 0x3

    invoke-direct {v3, v4, v5}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v3, Lcom/google/gson/p$a;->d:Lcom/google/gson/p$a;

    filled-new-array {v0, v1, v2, v3}, [Lcom/google/gson/p$a;

    move-result-object v0

    sput-object v0, Lcom/google/gson/p$a;->e:[Lcom/google/gson/p$a;

    return-void
.end method

.method public constructor <init>()V
    .locals 0

    const/4 p0, 0x0

    throw p0
.end method

.method public static valueOf(Ljava/lang/String;)Lcom/google/gson/p$a;
    .locals 1

    const-class v0, Lcom/google/gson/p$a;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object p0

    check-cast p0, Lcom/google/gson/p$a;

    return-object p0
.end method

.method public static values()[Lcom/google/gson/p$a;
    .locals 1

    sget-object v0, Lcom/google/gson/p$a;->e:[Lcom/google/gson/p$a;

    invoke-virtual {v0}, [Lcom/google/gson/p$a;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Lcom/google/gson/p$a;

    return-object v0
.end method
