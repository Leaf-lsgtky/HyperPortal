.class public Lcom/miui/contentcatcher/common/NodeInfo;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/io/Serializable;


# static fields
.field private static final serialVersionUID:J = 0x1L


# instance fields
.field private bitmap:Landroid/graphics/Bitmap;

.field private bitmapUri:Ljava/lang/String;

.field private clazz:Ljava/lang/String;

.field private extra:Ljava/util/HashMap;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field

.field private height:I

.field private id:Ljava/lang/String;

.field private isPureColor:Z

.field private text:Ljava/lang/String;

.field private type:I

.field private final uuid:Ljava/lang/String;

.field private webviewUrl:Ljava/lang/String;

.field private width:I

.field private x:F

.field private y:F


# direct methods
.method public constructor <init>()V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const-string v0, ""

    iput-object v0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->id:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->text:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->clazz:Ljava/lang/String;

    invoke-static {}, Ljava/util/UUID;->randomUUID()Ljava/util/UUID;

    move-result-object v0

    invoke-virtual {v0}, Ljava/util/UUID;->toString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->uuid:Ljava/lang/String;

    const/4 v0, -0x1

    iput v0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->type:I

    iput v0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->width:I

    iput v0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->height:I

    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    iput-object v0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->extra:Ljava/util/HashMap;

    return-void
.end method


# virtual methods
.method public getBitmap()Landroid/graphics/Bitmap;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->bitmap:Landroid/graphics/Bitmap;

    return-object p0
.end method

.method public getBitmapUri()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->bitmapUri:Ljava/lang/String;

    return-object p0
.end method

.method public getClazz()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->clazz:Ljava/lang/String;

    return-object p0
.end method

.method public getExtra()Ljava/util/HashMap;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    iget-object p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->extra:Ljava/util/HashMap;

    return-object p0
.end method

.method public getHeight()I
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->height:I

    return p0
.end method

.method public getId()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->id:Ljava/lang/String;

    return-object p0
.end method

.method public getText()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->text:Ljava/lang/String;

    return-object p0
.end method

.method public getType()I
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->type:I

    return p0
.end method

.method public getUuid()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->uuid:Ljava/lang/String;

    return-object p0
.end method

.method public getWebviewUrl()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->webviewUrl:Ljava/lang/String;

    return-object p0
.end method

.method public getWidth()I
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->width:I

    return p0
.end method

.method public getX()F
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->x:F

    return p0
.end method

.method public getY()F
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->y:F

    return p0
.end method

.method public isPureColor()Z
    .locals 0

    iget-boolean p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->isPureColor:Z

    return p0
.end method

.method public setBitmap(Landroid/graphics/Bitmap;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->bitmap:Landroid/graphics/Bitmap;

    return-void
.end method

.method public setBitmapUri(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->bitmapUri:Ljava/lang/String;

    return-void
.end method

.method public setClazz(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->clazz:Ljava/lang/String;

    return-void
.end method

.method public setExtra(Ljava/util/HashMap;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;)V"
        }
    .end annotation

    iput-object p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->extra:Ljava/util/HashMap;

    return-void
.end method

.method public setHeight(I)V
    .locals 0

    iput p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->height:I

    return-void
.end method

.method public setId(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->id:Ljava/lang/String;

    return-void
.end method

.method public setIsPureColor(Z)V
    .locals 0

    iput-boolean p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->isPureColor:Z

    return-void
.end method

.method public setText(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->text:Ljava/lang/String;

    return-void
.end method

.method public setType(I)V
    .locals 0

    iput p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->type:I

    return-void
.end method

.method public setWebviewUrl(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->webviewUrl:Ljava/lang/String;

    return-void
.end method

.method public setWidth(I)V
    .locals 0

    iput p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->width:I

    return-void
.end method

.method public setX(F)V
    .locals 0

    iput p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->x:F

    return-void
.end method

.method public setY(F)V
    .locals 0

    iput p1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->y:F

    return-void
.end method

.method public toString()Ljava/lang/String;
    .locals 2

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "NodeInfo{id=\'"

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->id:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "\', clazz=\'"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->clazz:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "\', uuid=\'"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->uuid:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "\', type="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->type:I

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v1, ", width="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->width:I

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v1, ", height="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->height:I

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v1, ", x="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->x:F

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;

    const-string v1, ", y="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v1, p0, Lcom/miui/contentcatcher/common/NodeInfo;->y:F

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;

    const-string v1, ", text=\'"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p0, p0, Lcom/miui/contentcatcher/common/NodeInfo;->text:Ljava/lang/String;

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p0, "\'}"

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method
