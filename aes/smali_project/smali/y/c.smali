.class public final synthetic Ly/c;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/util/function/Function;


# instance fields
.field public final synthetic a:Ly/g;


# direct methods
.method public synthetic constructor <init>(Ly/g;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Ly/c;->a:Ly/g;

    return-void
.end method


# virtual methods
.method public final apply(Ljava/lang/Object;)Ljava/lang/Object;
    .locals 6

    check-cast p1, Lcom/miui/contentcatcher/Interceptor$d;

    iget-object p0, p0, Ly/c;->a:Ly/g;

    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    if-nez p1, :cond_0

    const/4 p0, 0x0

    goto/16 :goto_4

    :cond_0
    iget-object v0, p1, Lcom/miui/contentcatcher/Interceptor$d;->b:Landroid/os/Bundle;

    const-string v1, "catchType"

    invoke-virtual {v0, v1}, Landroid/os/Bundle;->getInt(Ljava/lang/String;)I

    move-result v1

    new-instance v2, Lcom/miui/contentcatcher/common/NodeInfo;

    invoke-direct {v2}, Lcom/miui/contentcatcher/common/NodeInfo;-><init>()V

    invoke-virtual {v2, v1}, Lcom/miui/contentcatcher/common/NodeInfo;->setType(I)V

    const/4 v3, 0x1

    iget-object p1, p1, Lcom/miui/contentcatcher/Interceptor$d;->a:Landroid/view/View;

    if-eqz p1, :cond_5

    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v2, v4}, Lcom/miui/contentcatcher/common/NodeInfo;->setClazz(Ljava/lang/String;)V

    invoke-virtual {p1}, Landroid/view/View;->getId()I

    move-result v4

    const/4 v5, -0x1

    if-eq v4, v5, :cond_1

    :try_start_0
    iget-object p0, p0, Ly/g;->b:Landroid/content/Context;

    invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object p0

    invoke-virtual {p1}, Landroid/view/View;->getId()I

    move-result v4

    invoke-virtual {p0, v4}, Landroid/content/res/Resources;->getResourceEntryName(I)Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v2, p0}, Lcom/miui/contentcatcher/common/NodeInfo;->setId(Ljava/lang/String;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p0

    const-string v4, "convert id error=>"

    const-string v5, "SettingTrigger"

    invoke-static {v4, p0, v5}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_1
    :goto_0
    const/4 p0, 0x2

    new-array p0, p0, [I

    invoke-virtual {p1, p0}, Landroid/view/View;->getLocationOnScreen([I)V

    const/4 v4, 0x0

    aget v5, p0, v4

    int-to-float v5, v5

    invoke-virtual {v2, v5}, Lcom/miui/contentcatcher/common/NodeInfo;->setX(F)V

    aget p0, p0, v3

    int-to-float p0, p0

    invoke-virtual {v2, p0}, Lcom/miui/contentcatcher/common/NodeInfo;->setY(F)V

    invoke-virtual {p1}, Landroid/view/View;->getWidth()I

    move-result p0

    invoke-virtual {v2, p0}, Lcom/miui/contentcatcher/common/NodeInfo;->setWidth(I)V

    invoke-virtual {p1}, Landroid/view/View;->getHeight()I

    move-result p0

    invoke-virtual {v2, p0}, Lcom/miui/contentcatcher/common/NodeInfo;->setHeight(I)V

    sget-object p0, Lv/c;->a:Ljava/util/HashSet;

    invoke-virtual {p1}, Landroid/view/View;->isShown()Z

    move-result p0

    if-eqz p0, :cond_3

    invoke-virtual {p1}, Landroid/view/View;->getContext()Landroid/content/Context;

    move-result-object p0

    if-nez p0, :cond_2

    goto :goto_1

    :cond_2
    new-instance p0, Landroid/graphics/Rect;

    invoke-direct {p0}, Landroid/graphics/Rect;-><init>()V

    invoke-virtual {p1, p0}, Landroid/view/View;->getGlobalVisibleRect(Landroid/graphics/Rect;)Z

    move-result v4

    :cond_3
    :goto_1
    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getExtra()Ljava/util/HashMap;

    move-result-object p0

    if-eqz v4, :cond_4

    const-string p1, "1"

    goto :goto_2

    :cond_4
    const-string p1, "0"

    :goto_2
    const-string v4, "isVisible"

    invoke-virtual {p0, v4, p1}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :cond_5
    if-ne v1, v3, :cond_6

    const-string p0, "viewContent"

    invoke-virtual {v0, p0}, Landroid/os/Bundle;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v2, p0}, Lcom/miui/contentcatcher/common/NodeInfo;->setText(Ljava/lang/String;)V

    goto :goto_3

    :cond_6
    const-string p0, "viewBitmap"

    invoke-virtual {v0, p0}, Landroid/os/Bundle;->getParcelable(Ljava/lang/String;)Landroid/os/Parcelable;

    move-result-object p0

    check-cast p0, Landroid/graphics/Bitmap;

    invoke-virtual {v2, p0}, Lcom/miui/contentcatcher/common/NodeInfo;->setBitmap(Landroid/graphics/Bitmap;)V

    :goto_3
    move-object p0, v2

    :goto_4
    return-object p0
.end method
