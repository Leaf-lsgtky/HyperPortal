.class public final Ly/g$e;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/lang/Runnable;


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Ly/g;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x9
    name = "e"
.end annotation


# instance fields
.field public a:Ljava/lang/ref/WeakReference;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/lang/ref/WeakReference<",
            "Ly/g;",
            ">;"
        }
    .end annotation
.end field

.field public b:I


# virtual methods
.method public final run()V
    .locals 16

    move-object/from16 v0, p0

    const/4 v1, 0x2

    const-string v3, "SettingTrigger"

    const-string v4, "3 transferContentRunnable"

    invoke-static {v3, v4}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v4, v0, Ly/g$e;->a:Ljava/lang/ref/WeakReference;

    invoke-virtual {v4}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ly/g;

    if-eqz v4, :cond_11

    iget-object v5, v4, Ly/g;->c:Ljava/util/concurrent/CopyOnWriteArrayList;

    iget v0, v0, Ly/g$e;->b:I

    iget-boolean v6, v4, Ly/g;->f:Z

    const/4 v7, 0x3

    if-eqz v6, :cond_e

    iget-object v6, v4, Ly/g;->b:Landroid/content/Context;

    invoke-virtual {v6}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v6

    const-string v9, "content_catcher_global_collect_rule"

    invoke-static {v6, v9}, Landroid/provider/Settings$Secure;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v6

    iget-object v9, v4, Ly/g;->b:Landroid/content/Context;

    iget-object v10, v4, Ly/g;->a:Lcom/miui/contentcatcher/Interceptor;

    iget-object v10, v10, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    invoke-static {v5}, Ly/g;->e(Ljava/util/concurrent/CopyOnWriteArrayList;)Ljava/util/ArrayList;

    move-result-object v11

    iget-object v4, v4, Ly/g;->b:Landroid/content/Context;

    new-instance v12, Ljava/util/HashMap;

    invoke-direct {v12}, Ljava/util/HashMap;-><init>()V

    if-eqz v5, :cond_4

    invoke-virtual {v5}, Ljava/util/concurrent/CopyOnWriteArrayList;->isEmpty()Z

    move-result v13

    if-eqz v13, :cond_0

    goto :goto_2

    :cond_0
    sget-boolean v13, Lp/b;->a:Z

    if-eqz v13, :cond_1

    invoke-static {v4}, Lv/b;->a(Landroid/content/Context;)V

    :cond_1
    invoke-virtual {v5}, Ljava/util/concurrent/CopyOnWriteArrayList;->size()I

    move-result v13

    add-int/lit8 v13, v13, -0x1

    :goto_0
    if-ltz v13, :cond_4

    invoke-virtual {v5, v13}, Ljava/util/concurrent/CopyOnWriteArrayList;->get(I)Ljava/lang/Object;

    move-result-object v14

    check-cast v14, Lcom/miui/contentcatcher/common/NodeInfo;

    invoke-virtual {v14}, Lcom/miui/contentcatcher/common/NodeInfo;->getBitmap()Landroid/graphics/Bitmap;

    move-result-object v15

    if-eqz v15, :cond_3

    invoke-virtual {v15}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v8

    const/16 v2, 0x96

    if-lt v8, v2, :cond_3

    invoke-virtual {v15}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v8

    if-ge v8, v2, :cond_2

    goto :goto_1

    :cond_2
    invoke-virtual {v14}, Lcom/miui/contentcatcher/common/NodeInfo;->getUuid()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v12, v2, v15}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    sget-boolean v2, Lp/b;->a:Z

    if-eqz v2, :cond_3

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v8, "save image for info:"

    invoke-direct {v2, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v3, v2}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v14}, Lcom/miui/contentcatcher/common/NodeInfo;->getUuid()Ljava/lang/String;

    move-result-object v2

    invoke-static {v4, v15, v2}, Lv/b;->b(Landroid/content/Context;Landroid/graphics/Bitmap;Ljava/lang/String;)V

    :cond_3
    :goto_1
    add-int/lit8 v13, v13, -0x1

    goto :goto_0

    :cond_4
    :goto_2
    sget-object v2, Ly/a;->a:Landroid/net/Uri;

    const-string v2, "sendGlobalCollectData: "

    new-instance v3, Ljava/lang/StringBuilder;

    const-string v4, "sendGlobalCollectData fetchType="

    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v0, ", catchRule: "

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v0, ", activity="

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3, v10}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v0, ", bitmap size:"

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v12}, Ljava/util/HashMap;->size()I

    move-result v0

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v0, ", node size:"

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v11}, Ljava/util/ArrayList;->size()I

    move-result v0

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v3, "AiCrProviderManager"

    invoke-static {v3, v0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v4, Landroid/os/Bundle;

    invoke-direct {v4}, Landroid/os/Bundle;-><init>()V

    invoke-static {v6}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v0

    if-nez v0, :cond_5

    const/4 v0, 0x4

    goto :goto_3

    :cond_5
    move v0, v7

    :goto_3
    const-string v5, "image"

    const-string v8, "pushImgData: activity is illegal"

    if-ne v0, v7, :cond_7

    :try_start_0
    invoke-static {v4, v9, v10, v11, v0}, Ly/a;->d(Landroid/os/Bundle;Landroid/content/Context;Landroid/app/Activity;Ljava/util/ArrayList;I)V

    invoke-static {v4, v10}, Ly/a;->c(Landroid/os/Bundle;Landroid/app/Activity;)V

    invoke-virtual {v12}, Ljava/util/HashMap;->isEmpty()Z

    move-result v0

    if-eqz v0, :cond_6

    invoke-static {v3, v8}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    goto/16 :goto_6

    :cond_6
    new-instance v0, Ly/b;

    invoke-direct {v0, v12}, Ly/b;-><init>(Ljava/util/HashMap;)V

    invoke-virtual {v4, v5, v0}, Landroid/os/Bundle;->putBinder(Ljava/lang/String;Landroid/os/IBinder;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_6

    :catch_0
    move-exception v0

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v5, "version 3 sendGlobalCollectData e: "

    invoke-direct {v1, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v3, v0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_6

    :cond_7
    :try_start_1
    new-instance v7, Lorg/json/JSONObject;

    invoke-direct {v7, v6}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V

    const-string v6, "catchType"

    invoke-virtual {v7, v6}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;)I

    move-result v6

    if-nez v6, :cond_8

    const-string v0, "catchType is 0"

    invoke-static {v3, v0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_6

    :catch_1
    move-exception v0

    goto :goto_5

    :cond_8
    or-int/2addr v1, v6

    if-ne v1, v6, :cond_9

    invoke-static {v4, v9, v10, v11, v0}, Ly/a;->d(Landroid/os/Bundle;Landroid/content/Context;Landroid/app/Activity;Ljava/util/ArrayList;I)V

    :cond_9
    or-int/lit8 v0, v6, 0x1

    if-ne v0, v6, :cond_a

    invoke-static {v4, v10}, Ly/a;->c(Landroid/os/Bundle;Landroid/app/Activity;)V

    :cond_a
    const/4 v0, 0x4

    or-int/2addr v0, v6

    if-ne v0, v6, :cond_c

    invoke-virtual {v12}, Ljava/util/HashMap;->isEmpty()Z

    move-result v0

    if-eqz v0, :cond_b

    invoke-static {v3, v8}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_4

    :cond_b
    new-instance v0, Ly/b;

    invoke-direct {v0, v12}, Ly/b;-><init>(Ljava/util/HashMap;)V

    invoke-virtual {v4, v5, v0}, Landroid/os/Bundle;->putBinder(Ljava/lang/String;Landroid/os/IBinder;)V

    :cond_c
    :goto_4
    or-int/lit8 v0, v6, 0x8

    if-ne v0, v6, :cond_d

    invoke-static {v4, v7, v10}, Ly/a;->b(Landroid/os/Bundle;Lorg/json/JSONObject;Landroid/app/Activity;)V
    :try_end_1
    .catch Lorg/json/JSONException; {:try_start_1 .. :try_end_1} :catch_1
    .catch Ljava/lang/NoSuchFieldException; {:try_start_1 .. :try_end_1} :catch_1
    .catch Ljava/lang/IllegalAccessException; {:try_start_1 .. :try_end_1} :catch_1
    .catch Ljava/lang/NullPointerException; {:try_start_1 .. :try_end_1} :catch_1

    goto :goto_6

    :goto_5
    new-instance v1, Ljava/lang/StringBuilder;

    const-string v5, "sendGlobalCollectData e: "

    invoke-direct {v1, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v3, v0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    :cond_d
    :goto_6
    :try_start_2
    const-string v0, "sendGlobalCollectData, traceTime end catch: send global collect data"

    invoke-static {v3, v0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v4}, Landroid/os/Bundle;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v3, v0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v9}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v0

    sget-object v1, Ly/a;->a:Landroid/net/Uri;

    const-string v2, "doGlobalCollectTransfer"

    const/4 v5, 0x0

    invoke-virtual {v0, v1, v2, v5, v4}, Landroid/content/ContentResolver;->call(Landroid/net/Uri;Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;)Landroid/os/Bundle;
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_2

    goto :goto_7

    :catch_2
    move-exception v0

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "sendGlobalCollectData doGlobalCollectTransfer error:"

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v3, v0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_7

    :cond_e
    if-ne v0, v1, :cond_f

    iget-object v1, v4, Ly/g;->b:Landroid/content/Context;

    invoke-static {v5}, Ly/g;->e(Ljava/util/concurrent/CopyOnWriteArrayList;)Ljava/util/ArrayList;

    move-result-object v2

    const/4 v3, 0x0

    invoke-static {v1, v0, v2, v3}, Ly/a;->e(Landroid/content/Context;ILjava/util/ArrayList;Lcom/miui/contentcatcher/common/NodeInfo;)V

    goto :goto_7

    :cond_f
    const/4 v3, 0x0

    if-ne v0, v7, :cond_10

    iget-object v1, v4, Ly/g;->b:Landroid/content/Context;

    invoke-static {v1, v5}, Ly/g;->c(Landroid/content/Context;Ljava/util/concurrent/CopyOnWriteArrayList;)Lcom/miui/contentcatcher/common/NodeInfo;

    move-result-object v2

    invoke-static {v1, v0, v3, v2}, Ly/a;->e(Landroid/content/Context;ILjava/util/ArrayList;Lcom/miui/contentcatcher/common/NodeInfo;)V

    goto :goto_7

    :cond_10
    const/4 v1, 0x4

    if-ne v0, v1, :cond_12

    iget-object v1, v4, Ly/g;->b:Landroid/content/Context;

    invoke-static {v5}, Ly/g;->e(Ljava/util/concurrent/CopyOnWriteArrayList;)Ljava/util/ArrayList;

    move-result-object v2

    iget-object v3, v4, Ly/g;->b:Landroid/content/Context;

    invoke-static {v3, v5}, Ly/g;->c(Landroid/content/Context;Ljava/util/concurrent/CopyOnWriteArrayList;)Lcom/miui/contentcatcher/common/NodeInfo;

    move-result-object v3

    invoke-static {v1, v0, v2, v3}, Ly/a;->e(Landroid/content/Context;ILjava/util/ArrayList;Lcom/miui/contentcatcher/common/NodeInfo;)V

    goto :goto_7

    :cond_11
    const-string v0, "TransferContentRunnable run settingTrigger is null"

    invoke-static {v3, v0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    :cond_12
    :goto_7
    return-void
.end method
