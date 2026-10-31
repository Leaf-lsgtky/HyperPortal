.class public final enum Lj/b;
.super Ljava/lang/Enum;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum<",
        "Lj/b;",
        ">;"
    }
.end annotation


# static fields
.field public static final enum a:Lj/b;

.field public static final enum b:Lj/b;

.field public static final enum c:Lj/b;

.field public static final enum d:Lj/b;

.field public static final enum e:Lj/b;

.field public static final enum f:Lj/b;

.field public static final enum g:Lj/b;

.field public static final enum h:Lj/b;

.field public static final enum i:Lj/b;

.field public static final enum j:Lj/b;

.field public static final synthetic k:[Lj/b;


# direct methods
.method static constructor <clinit>()V
    .locals 12

    new-instance v0, Lj/b;

    const-string v1, "BEGIN_ARRAY"

    const/4 v2, 0x0

    invoke-direct {v0, v1, v2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v0, Lj/b;->a:Lj/b;

    new-instance v1, Lj/b;

    const-string v2, "END_ARRAY"

    const/4 v3, 0x1

    invoke-direct {v1, v2, v3}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v1, Lj/b;->b:Lj/b;

    new-instance v2, Lj/b;

    const-string v3, "BEGIN_OBJECT"

    const/4 v4, 0x2

    invoke-direct {v2, v3, v4}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v2, Lj/b;->c:Lj/b;

    new-instance v3, Lj/b;

    const-string v4, "END_OBJECT"

    const/4 v5, 0x3

    invoke-direct {v3, v4, v5}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v3, Lj/b;->d:Lj/b;

    new-instance v4, Lj/b;

    const-string v5, "NAME"

    const/4 v6, 0x4

    invoke-direct {v4, v5, v6}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v4, Lj/b;->e:Lj/b;

    new-instance v5, Lj/b;

    const-string v6, "STRING"

    const/4 v7, 0x5

    invoke-direct {v5, v6, v7}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v5, Lj/b;->f:Lj/b;

    new-instance v6, Lj/b;

    const-string v7, "NUMBER"

    const/4 v8, 0x6

    invoke-direct {v6, v7, v8}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v6, Lj/b;->g:Lj/b;

    new-instance v7, Lj/b;

    const-string v8, "BOOLEAN"

    const/4 v9, 0x7

    invoke-direct {v7, v8, v9}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v7, Lj/b;->h:Lj/b;

    new-instance v8, Lj/b;

    const-string v9, "NULL"

    const/16 v10, 0x8

    invoke-direct {v8, v9, v10}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v8, Lj/b;->i:Lj/b;

    new-instance v9, Lj/b;

    const-string v10, "END_DOCUMENT"

    const/16 v11, 0x9

    invoke-direct {v9, v10, v11}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    sput-object v9, Lj/b;->j:Lj/b;

    filled-new-array/range {v0 .. v9}, [Lj/b;

    move-result-object v0

    sput-object v0, Lj/b;->k:[Lj/b;

    return-void
.end method

.method public constructor <init>()V
    .locals 0

    const/4 p0, 0x0

    throw p0
.end method

.method public static valueOf(Ljava/lang/String;)Lj/b;
    .locals 1

    const-class v0, Lj/b;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object p0

    check-cast p0, Lj/b;

    return-object p0
.end method

.method public static values()[Lj/b;
    .locals 1

    sget-object v0, Lj/b;->k:[Lj/b;

    invoke-virtual {v0}, [Lj/b;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Lj/b;

    return-object v0
.end method
