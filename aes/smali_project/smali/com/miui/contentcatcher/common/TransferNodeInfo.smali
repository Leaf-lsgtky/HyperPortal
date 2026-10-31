.class public Lcom/miui/contentcatcher/common/TransferNodeInfo;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/io/Serializable;


# static fields
.field private static final serialVersionUID:J = 0x1de38f784cd23L


# instance fields
.field private bitmapUri:Ljava/lang/String;

.field private className:Ljava/lang/String;

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

.field private text:Ljava/lang/String;

.field private type:I

.field private uuid:Ljava/lang/String;

.field private webviewUrl:Ljava/lang/String;

.field private width:I

.field private x:F

.field private y:F


# direct methods
.method public constructor <init>()V
    .locals 1

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const/4 v0, 0x0

    .line 2
    iput v0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->type:I

    .line 3
    iput v0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->width:I

    .line 4
    iput v0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->height:I

    const/4 v0, 0x0

    .line 5
    iput v0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->x:F

    .line 6
    iput v0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->y:F

    .line 7
    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    iput-object v0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->extra:Ljava/util/HashMap;

    return-void
.end method

.method public constructor <init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    .locals 1

    .line 8
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const/4 v0, 0x0

    .line 9
    iput v0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->type:I

    .line 10
    iput v0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->width:I

    .line 11
    iput v0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->height:I

    const/4 v0, 0x0

    .line 12
    iput v0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->x:F

    .line 13
    iput v0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->y:F

    .line 14
    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    iput-object v0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->extra:Ljava/util/HashMap;

    .line 15
    iput-object p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->id:Ljava/lang/String;

    .line 16
    iput-object p2, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->text:Ljava/lang/String;

    .line 17
    iput-object p3, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->className:Ljava/lang/String;

    return-void
.end method


# virtual methods
.method public getBitmapUri()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->bitmapUri:Ljava/lang/String;

    return-object p0
.end method

.method public getClassName()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->className:Ljava/lang/String;

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

    iget-object p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->extra:Ljava/util/HashMap;

    return-object p0
.end method

.method public getHeight()I
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->height:I

    return p0
.end method

.method public getId()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->id:Ljava/lang/String;

    return-object p0
.end method

.method public getText()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->text:Ljava/lang/String;

    return-object p0
.end method

.method public getType()I
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->type:I

    return p0
.end method

.method public getUuid()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->uuid:Ljava/lang/String;

    return-object p0
.end method

.method public getWebviewUrl()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->webviewUrl:Ljava/lang/String;

    return-object p0
.end method

.method public getWidth()I
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->width:I

    return p0
.end method

.method public getX()F
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->x:F

    return p0
.end method

.method public getY()F
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->y:F

    return p0
.end method

.method public setBitmapUri(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->bitmapUri:Ljava/lang/String;

    return-void
.end method

.method public setClassName(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->className:Ljava/lang/String;

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

    iput-object p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->extra:Ljava/util/HashMap;

    return-void
.end method

.method public setHeight(I)V
    .locals 0

    iput p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->height:I

    return-void
.end method

.method public setId(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->id:Ljava/lang/String;

    return-void
.end method

.method public setText(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->text:Ljava/lang/String;

    return-void
.end method

.method public setType(I)V
    .locals 0

    iput p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->type:I

    return-void
.end method

.method public setUuid(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->uuid:Ljava/lang/String;

    return-void
.end method

.method public setWebviewUrl(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->webviewUrl:Ljava/lang/String;

    return-void
.end method

.method public setWidth(I)V
    .locals 0

    iput p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->width:I

    return-void
.end method

.method public setX(F)V
    .locals 0

    iput p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->x:F

    return-void
.end method

.method public setY(F)V
    .locals 0

    iput p1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->y:F

    return-void
.end method

.method public toJsonObj()Lorg/json/JSONObject;
    .locals 4
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Lorg/json/JSONException;
        }
    .end annotation

    new-instance v0, Lorg/json/JSONObject;

    invoke-direct {v0}, Lorg/json/JSONObject;-><init>()V

    const-string v1, "uuid"

    invoke-virtual {p0}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->getUuid()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string v1, "id"

    invoke-virtual {p0}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->getId()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string v1, "text"

    invoke-virtual {p0}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->getText()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string v1, "className"

    invoke-virtual {p0}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->getClassName()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    invoke-virtual {p0}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->getX()F

    move-result v1

    float-to-double v1, v1

    const-string v3, "x"

    invoke-virtual {v0, v3, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;D)Lorg/json/JSONObject;

    invoke-virtual {p0}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->getY()F

    move-result v1

    float-to-double v1, v1

    const-string v3, "y"

    invoke-virtual {v0, v3, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;D)Lorg/json/JSONObject;

    const-string v1, "width"

    invoke-virtual {p0}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->getWidth()I

    move-result v2

    invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    const-string v1, "height"

    invoke-virtual {p0}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->getHeight()I

    move-result v2

    invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    const-string v1, "bitmapUri"

    invoke-virtual {p0}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->getBitmapUri()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string v1, "webviewUrl"

    invoke-virtual {p0}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->getWebviewUrl()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string v1, "type"

    invoke-virtual {p0}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->getType()I

    move-result v2

    invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    const-string v1, "extra"

    invoke-virtual {p0}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->getExtra()Ljava/util/HashMap;

    move-result-object p0

    invoke-virtual {v0, v1, p0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    return-object v0
.end method

.method public toString()Ljava/lang/String;
    .locals 2

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "TransferNodeInfo{id=\'"

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->id:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "\', className=\'"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->className:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "\', uuid=\'"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->uuid:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "\', bitmapUri=\'"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->bitmapUri:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "\', webviewUrl=\'"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->webviewUrl:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "\', type="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->type:I

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v1, ", width="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->width:I

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v1, ", height="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->height:I

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v1, ", x="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->x:F

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;

    const-string v1, ", y="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->y:F

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;

    const-string v1, ", extra="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->extra:Ljava/util/HashMap;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v1, ", text=\'"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p0, p0, Lcom/miui/contentcatcher/common/TransferNodeInfo;->text:Ljava/lang/String;

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p0, "\'}"

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method
