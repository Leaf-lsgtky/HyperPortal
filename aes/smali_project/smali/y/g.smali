.class public final Ly/g;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Ly/g$e;,
        Ly/g$c;,
        Ly/g$d;
    }
.end annotation


# static fields
.field public static i:J


# instance fields
.field public a:Lcom/miui/contentcatcher/Interceptor;

.field public b:Landroid/content/Context;

.field public final c:Ljava/util/concurrent/CopyOnWriteArrayList;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/concurrent/CopyOnWriteArrayList<",
            "Lcom/miui/contentcatcher/common/NodeInfo;",
            ">;"
        }
    .end annotation
.end field

.field public final d:Landroid/os/Handler;

.field public e:Ljava/util/concurrent/atomic/AtomicInteger;

.field public f:Z

.field public final g:Ly/g$a;

.field public final h:Ly/g$e;


# direct methods
.method public constructor <init>()V
    .locals 3

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    new-instance v0, Ljava/util/concurrent/CopyOnWriteArrayList;

    invoke-direct {v0}, Ljava/util/concurrent/CopyOnWriteArrayList;-><init>()V

    iput-object v0, p0, Ly/g;->c:Ljava/util/concurrent/CopyOnWriteArrayList;

    new-instance v0, Landroid/os/Handler;

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    move-result-object v1

    invoke-direct {v0, v1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    iput-object v0, p0, Ly/g;->d:Landroid/os/Handler;

    new-instance v0, Ljava/util/concurrent/atomic/AtomicInteger;

    const/4 v1, 0x2

    invoke-direct {v0, v1}, Ljava/util/concurrent/atomic/AtomicInteger;-><init>(I)V

    iput-object v0, p0, Ly/g;->e:Ljava/util/concurrent/atomic/AtomicInteger;

    const/4 v0, 0x0

    iput-boolean v0, p0, Ly/g;->f:Z

    new-instance v0, Ly/g$a;

    invoke-direct {v0, p0}, Ly/g$a;-><init>(Ly/g;)V

    iput-object v0, p0, Ly/g;->g:Ly/g$a;

    new-instance v0, Ly/g$b;

    new-instance v1, Landroid/os/Handler;

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    move-result-object v2

    invoke-direct {v1, v2}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    invoke-direct {v0, p0, v1}, Ly/g$b;-><init>(Ly/g;Landroid/os/Handler;)V

    new-instance v0, Ly/g$e;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    new-instance v1, Ljava/lang/ref/WeakReference;

    invoke-direct {v1, p0}, Ljava/lang/ref/WeakReference;-><init>(Ljava/lang/Object;)V

    iput-object v1, v0, Ly/g$e;->a:Ljava/lang/ref/WeakReference;

    iput-object v0, p0, Ly/g;->h:Ly/g$e;

    return-void
.end method

.method public static a(ILy/g;)V
    .locals 12

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "fetch=>Start type = "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v1, " pkgName = "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p1, Ly/g;->b:Landroid/content/Context;

    invoke-virtual {v1}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "SettingTrigger"

    invoke-static {v1, v0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v0, p1, Ly/g;->d:Landroid/os/Handler;

    iget-object v2, p1, Ly/g;->c:Ljava/util/concurrent/CopyOnWriteArrayList;

    const-wide/16 v3, 0xc8

    const/4 v5, 0x1

    const-wide/16 v6, 0x0

    iget-object v8, p1, Ly/g;->h:Ly/g$e;

    const/4 v9, 0x2

    if-eq p0, v9, :cond_4

    const/4 v10, 0x3

    if-eq p0, v10, :cond_2

    const/4 v3, 0x4

    if-eq p0, v3, :cond_0

    goto/16 :goto_2

    :cond_0
    new-instance p0, Ljava/util/concurrent/atomic/AtomicInteger;

    invoke-direct {p0, v9}, Ljava/util/concurrent/atomic/AtomicInteger;-><init>(I)V

    iput-object p0, p1, Ly/g;->e:Ljava/util/concurrent/atomic/AtomicInteger;

    const-string p0, "fetch=>AllStart"

    invoke-static {v1, p0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v2}, Ljava/util/concurrent/CopyOnWriteArrayList;->clear()V

    new-instance p0, Ljava/util/ArrayList;

    invoke-direct {p0}, Ljava/util/ArrayList;-><init>()V

    iget-object v4, p1, Ly/g;->a:Lcom/miui/contentcatcher/Interceptor;

    invoke-virtual {v4, p0, v10}, Lcom/miui/contentcatcher/Interceptor;->f(Ljava/util/ArrayList;I)Z

    move-result v4

    new-instance v5, Ljava/lang/StringBuilder;

    const-string v9, "fetchAll add node size: "

    invoke-direct {v5, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/util/ArrayList;->size()I

    move-result v9

    invoke-virtual {v5, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-static {v1, v5}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {p1, p0}, Ly/g;->b(Ljava/util/ArrayList;)Ljava/util/List;

    move-result-object p0

    invoke-virtual {v2, p0}, Ljava/util/concurrent/CopyOnWriteArrayList;->addAll(Ljava/util/Collection;)Z

    new-instance p0, Ljava/lang/StringBuilder;

    const-string p1, "fetch=>AllEnd hasWebView = "

    invoke-direct {p0, p1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, v4}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v1, p0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iput v3, v8, Ly/g$e;->b:I

    if-eqz v4, :cond_1

    const-wide/16 v6, 0x12c

    :cond_1
    invoke-virtual {v0, v8, v6, v7}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    goto/16 :goto_2

    :cond_2
    new-instance p0, Ljava/util/concurrent/atomic/AtomicInteger;

    invoke-direct {p0, v5}, Ljava/util/concurrent/atomic/AtomicInteger;-><init>(I)V

    iput-object p0, p1, Ly/g;->e:Ljava/util/concurrent/atomic/AtomicInteger;

    invoke-virtual {v2}, Ljava/util/concurrent/CopyOnWriteArrayList;->clear()V

    const-string p0, "fetch=>ImageStart"

    invoke-static {v1, p0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p0, Ljava/util/ArrayList;

    invoke-direct {p0}, Ljava/util/ArrayList;-><init>()V

    iget-object v5, p1, Ly/g;->a:Lcom/miui/contentcatcher/Interceptor;

    invoke-virtual {v5, p0, v9}, Lcom/miui/contentcatcher/Interceptor;->f(Ljava/util/ArrayList;I)Z

    move-result v5

    new-instance v9, Ljava/lang/StringBuilder;

    const-string v11, "fetchImage add node size: "

    invoke-direct {v9, v11}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/util/ArrayList;->size()I

    move-result v11

    invoke-virtual {v9, v11}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v9}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v9

    invoke-static {v1, v9}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {p1, p0}, Ly/g;->b(Ljava/util/ArrayList;)Ljava/util/List;

    move-result-object p0

    invoke-virtual {v2, p0}, Ljava/util/concurrent/CopyOnWriteArrayList;->addAll(Ljava/util/Collection;)Z

    const-string p0, "fetchImage=>end"

    invoke-static {v1, p0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    iput v10, v8, Ly/g$e;->b:I

    if-eqz v5, :cond_3

    goto :goto_0

    :cond_3
    move-wide v3, v6

    :goto_0
    invoke-virtual {v0, v8, v3, v4}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    goto :goto_2

    :cond_4
    new-instance p0, Ljava/util/concurrent/atomic/AtomicInteger;

    invoke-direct {p0, v5}, Ljava/util/concurrent/atomic/AtomicInteger;-><init>(I)V

    iput-object p0, p1, Ly/g;->e:Ljava/util/concurrent/atomic/AtomicInteger;

    const-string p0, "fetchText=>start"

    invoke-static {v1, p0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v2}, Ljava/util/concurrent/CopyOnWriteArrayList;->clear()V

    new-instance p0, Ljava/util/ArrayList;

    invoke-direct {p0}, Ljava/util/ArrayList;-><init>()V

    iget-object v10, p1, Ly/g;->a:Lcom/miui/contentcatcher/Interceptor;

    invoke-virtual {v10, p0, v5}, Lcom/miui/contentcatcher/Interceptor;->f(Ljava/util/ArrayList;I)Z

    move-result v5

    new-instance v10, Ljava/lang/StringBuilder;

    const-string v11, "fetchText add node size: "

    invoke-direct {v10, v11}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/util/ArrayList;->size()I

    move-result v11

    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v10

    invoke-static {v1, v10}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {p1, p0}, Ly/g;->b(Ljava/util/ArrayList;)Ljava/util/List;

    move-result-object p0

    invoke-virtual {v2, p0}, Ljava/util/concurrent/CopyOnWriteArrayList;->addAll(Ljava/util/Collection;)Z

    new-instance p0, Ljava/lang/StringBuilder;

    const-string p1, "fetchText=>end hasWebView = "

    invoke-direct {p0, p1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, v5}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v1, p0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    iput v9, v8, Ly/g$e;->b:I

    if-eqz v5, :cond_5

    goto :goto_1

    :cond_5
    move-wide v3, v6

    :goto_1
    invoke-virtual {v0, v8, v3, v4}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    :goto_2
    return-void
.end method

.method public static c(Landroid/content/Context;Ljava/util/concurrent/CopyOnWriteArrayList;)Lcom/miui/contentcatcher/common/NodeInfo;
    .locals 12

    if-eqz p1, :cond_7

    invoke-interface {p1}, Ljava/util/List;->isEmpty()Z

    move-result v0

    if-eqz v0, :cond_0

    goto/16 :goto_3

    :cond_0
    invoke-interface {p1}, Ljava/util/List;->size()I

    move-result v0

    add-int/lit8 v1, v0, -0x1

    invoke-interface {p1, v1}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lcom/miui/contentcatcher/common/NodeInfo;

    invoke-virtual {v1}, Lcom/miui/contentcatcher/common/NodeInfo;->getBitmap()Landroid/graphics/Bitmap;

    move-result-object v2

    sget-boolean v3, Lp/b;->a:Z

    const-string v4, "save cur image to find largest image for info:"

    const-string v5, "SettingTrigger"

    if-eqz v3, :cond_1

    invoke-static {p0}, Lv/b;->a(Landroid/content/Context;)V

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v5, v3}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v1}, Lcom/miui/contentcatcher/common/NodeInfo;->getUuid()Ljava/lang/String;

    move-result-object v3

    invoke-static {p0, v2, v3}, Lv/b;->b(Landroid/content/Context;Landroid/graphics/Bitmap;Ljava/lang/String;)V

    :cond_1
    const/4 v3, 0x0

    if-nez v2, :cond_2

    move v2, v3

    goto :goto_0

    :cond_2
    invoke-virtual {v2}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v6

    invoke-virtual {v2}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v2

    mul-int/2addr v2, v6

    :goto_0
    new-instance v6, Ljava/lang/StringBuilder;

    const-string v7, "findLargestBitmap id: "

    invoke-direct {v6, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1}, Lcom/miui/contentcatcher/common/NodeInfo;->getId()Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v7, ", largestArea: "

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v7, ", class:"

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Lcom/miui/contentcatcher/common/NodeInfo;->getClazz()Ljava/lang/String;

    move-result-object v8

    invoke-virtual {v6, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v6

    invoke-static {v5, v6}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    add-int/lit8 v0, v0, -0x2

    :goto_1
    if-ltz v0, :cond_6

    invoke-interface {p1, v0}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Lcom/miui/contentcatcher/common/NodeInfo;

    invoke-virtual {v6}, Lcom/miui/contentcatcher/common/NodeInfo;->getBitmap()Landroid/graphics/Bitmap;

    move-result-object v8

    if-nez v8, :cond_3

    move v10, v3

    goto :goto_2

    :cond_3
    invoke-virtual {v8}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v9

    invoke-virtual {v8}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v10

    mul-int/2addr v10, v9

    :goto_2
    new-instance v9, Ljava/lang/StringBuilder;

    const-string v11, "findLargestBitmap current id: "

    invoke-direct {v9, v11}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v6}, Lcom/miui/contentcatcher/common/NodeInfo;->getId()Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v9, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v11, ", currentArea: "

    invoke-virtual {v9, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v9, v10}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v9, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Lcom/miui/contentcatcher/common/NodeInfo;->getClazz()Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v9, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v9}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v9

    invoke-static {v5, v9}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    sget-boolean v9, Lp/b;->a:Z

    if-eqz v9, :cond_4

    new-instance v9, Ljava/lang/StringBuilder;

    invoke-direct {v9, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v9, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v9}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v9

    invoke-static {v5, v9}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v6}, Lcom/miui/contentcatcher/common/NodeInfo;->getUuid()Ljava/lang/String;

    move-result-object v9

    invoke-static {p0, v8, v9}, Lv/b;->b(Landroid/content/Context;Landroid/graphics/Bitmap;Ljava/lang/String;)V

    :cond_4
    if-le v10, v2, :cond_5

    move-object v1, v6

    move v2, v10

    :cond_5
    add-int/lit8 v0, v0, -0x1

    goto :goto_1

    :cond_6
    new-instance p0, Ljava/lang/StringBuilder;

    const-string p1, "findLargestBitmap info: "

    invoke-direct {p0, p1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v5, p0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    return-object v1

    :cond_7
    :goto_3
    const/4 p0, 0x0

    return-object p0
.end method

.method public static e(Ljava/util/concurrent/CopyOnWriteArrayList;)Ljava/util/ArrayList;
    .locals 6

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    if-eqz p0, :cond_8

    invoke-interface {p0}, Ljava/util/List;->isEmpty()Z

    move-result v1

    if-eqz v1, :cond_0

    goto/16 :goto_2

    :cond_0
    invoke-interface {p0}, Ljava/util/List;->size()I

    move-result v1

    add-int/lit8 v1, v1, -0x1

    :goto_0
    if-ltz v1, :cond_8

    invoke-interface {p0, v1}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lcom/miui/contentcatcher/common/NodeInfo;

    if-nez v2, :cond_1

    goto/16 :goto_1

    :cond_1
    new-instance v3, Lcom/miui/contentcatcher/common/TransferNodeInfo;

    invoke-direct {v3}, Lcom/miui/contentcatcher/common/TransferNodeInfo;-><init>()V

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getId()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->setId(Ljava/lang/String;)V

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getText()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->setText(Ljava/lang/String;)V

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getClazz()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->setClassName(Ljava/lang/String;)V

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getUuid()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->setUuid(Ljava/lang/String;)V

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getX()F

    move-result v4

    invoke-virtual {v3, v4}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->setX(F)V

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getY()F

    move-result v4

    invoke-virtual {v3, v4}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->setY(F)V

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getBitmapUri()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v4

    if-nez v4, :cond_2

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getBitmapUri()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->setBitmapUri(Ljava/lang/String;)V

    :cond_2
    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getWebviewUrl()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v4

    if-nez v4, :cond_3

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getWebviewUrl()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->setWebviewUrl(Ljava/lang/String;)V

    :cond_3
    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getType()I

    move-result v4

    const/4 v5, -0x1

    if-eq v4, v5, :cond_4

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getType()I

    move-result v4

    invoke-virtual {v3, v4}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->setType(I)V

    :cond_4
    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getWidth()I

    move-result v4

    if-eq v4, v5, :cond_5

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getWidth()I

    move-result v4

    invoke-virtual {v3, v4}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->setWidth(I)V

    :cond_5
    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getHeight()I

    move-result v4

    if-eq v4, v5, :cond_6

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getHeight()I

    move-result v4

    invoke-virtual {v3, v4}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->setHeight(I)V

    :cond_6
    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getExtra()Ljava/util/HashMap;

    move-result-object v4

    invoke-virtual {v4}, Ljava/util/HashMap;->size()I

    move-result v4

    if-lez v4, :cond_7

    invoke-virtual {v2}, Lcom/miui/contentcatcher/common/NodeInfo;->getExtra()Ljava/util/HashMap;

    move-result-object v2

    invoke-virtual {v3, v2}, Lcom/miui/contentcatcher/common/TransferNodeInfo;->setExtra(Ljava/util/HashMap;)V

    :cond_7
    invoke-virtual {v0, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :goto_1
    add-int/lit8 v1, v1, -0x1

    goto/16 :goto_0

    :cond_8
    :goto_2
    return-object v0
.end method


# virtual methods
.method public final b(Ljava/util/ArrayList;)Ljava/util/List;
    .locals 1

    invoke-interface {p1}, Ljava/util/Collection;->stream()Ljava/util/stream/Stream;

    move-result-object p1

    new-instance v0, Ly/c;

    invoke-direct {v0, p0}, Ly/c;-><init>(Ly/g;)V

    invoke-interface {p1, v0}, Ljava/util/stream/Stream;->map(Ljava/util/function/Function;)Ljava/util/stream/Stream;

    move-result-object p0

    new-instance p1, Ly/d;

    invoke-direct {p1}, Ljava/lang/Object;-><init>()V

    invoke-interface {p0, p1}, Ljava/util/stream/Stream;->filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;

    move-result-object p0

    invoke-static {}, Ljava/util/stream/Collectors;->toList()Ljava/util/stream/Collector;

    move-result-object p1

    invoke-interface {p0, p1}, Ljava/util/stream/Stream;->collect(Ljava/util/stream/Collector;)Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Ljava/util/List;

    return-object p0
.end method

.method public final d(Landroid/net/Uri;)V
    .locals 8

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "receive onChange uri: "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "SettingTrigger"

    invoke-static {v1, v0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v2

    sget-wide v4, Ly/g;->i:J

    sub-long v4, v2, v4

    const-wide/16 v6, 0x12c

    cmp-long v0, v4, v6

    if-gez v0, :cond_0

    const-string p0, "traceTime, interval so short, return"

    invoke-static {v1, p0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    return-void

    :cond_0
    sput-wide v2, Ly/g;->i:J

    if-eqz p1, :cond_1

    const-string v0, "content_catcher_global_collect_status"

    invoke-static {v0}, Landroid/provider/Settings$Secure;->getUriFor(Ljava/lang/String;)Landroid/net/Uri;

    move-result-object v0

    invoke-virtual {v0, p1}, Landroid/net/Uri;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_1

    const/4 p1, 0x1

    goto :goto_0

    :cond_1
    const/4 p1, 0x0

    :goto_0
    iput-boolean p1, p0, Ly/g;->f:Z

    new-instance p1, Ljava/lang/StringBuilder;

    const-string v0, "traceTime start catch: receive catcher request, mIsGlobalCollect:"

    invoke-direct {p1, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-boolean v0, p0, Ly/g;->f:Z

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v1, p1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p1, Ly/g$c;

    invoke-direct {p1, p0}, Ly/g$c;-><init>(Ly/g;)V

    sget-object p0, Lx/b;->a:Ljava/util/concurrent/ThreadPoolExecutor;

    invoke-virtual {p0, p1}, Ljava/util/concurrent/ThreadPoolExecutor;->execute(Ljava/lang/Runnable;)V

    return-void
.end method
