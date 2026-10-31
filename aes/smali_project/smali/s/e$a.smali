.class public final enum Ls/e$a;
.super Ljava/lang/Enum;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Ls/e;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x4019
    name = "a"
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum<",
        "Ls/e$a;",
        ">;"
    }
.end annotation


# static fields
.field public static final enum a:Ls/e$a;

.field public static final enum b:Ls/e$a;

.field public static final enum c:Ls/e$a;

.field public static final synthetic d:[Ls/e$a;


# direct methods
.method static constructor <clinit>()V
    .locals 5

    new-instance v0, Ls/e$a;

    const-string v1, "StateStart"

    const/4 v2, 0x0

    invoke-direct {v0, v1, v2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v0, Ls/e$a;->a:Ls/e$a;

    new-instance v1, Ls/e$a;

    const-string v2, "StateLoading"

    const/4 v3, 0x1

    invoke-direct {v1, v2, v3}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v1, Ls/e$a;->b:Ls/e$a;

    new-instance v2, Ls/e$a;

    const-string v3, "StateFinished"

    const/4 v4, 0x2

    invoke-direct {v2, v3, v4}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v2, Ls/e$a;->c:Ls/e$a;

    filled-new-array {v0, v1, v2}, [Ls/e$a;

    move-result-object v0

    sput-object v0, Ls/e$a;->d:[Ls/e$a;

    return-void
.end method

.method public constructor <init>()V
    .locals 0

    const/4 p0, 0x0

    throw p0
.end method

.method public static valueOf(Ljava/lang/String;)Ls/e$a;
    .locals 1

    const-class v0, Ls/e$a;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object p0

    check-cast p0, Ls/e$a;

    return-object p0
.end method

.method public static values()[Ls/e$a;
    .locals 1

    sget-object v0, Ls/e$a;->d:[Ls/e$a;

    invoke-virtual {v0}, [Ls/e$a;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Ls/e$a;

    return-object v0
.end method
