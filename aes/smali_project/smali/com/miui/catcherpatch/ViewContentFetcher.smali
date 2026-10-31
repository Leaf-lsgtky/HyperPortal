.class public Lcom/miui/catcherpatch/ViewContentFetcher;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lmiui/contentcatcher/IViewContentFetcher;


# static fields
.field public static final BUNDLE_CATCH_TYPE:Ljava/lang/String; = "catchType"

.field public static final BUNDLE_VIEW_BITMAP:Ljava/lang/String; = "viewBitmap"

.field public static final BUNDLE_VIEW_CONTENT:Ljava/lang/String; = "viewContent"

.field private static final BUNDLE_VIEW_PURE_COLOR:Ljava/lang/String; = "isPureColor"

.field public static final CONTENT_TYPE_BITMAP:I = 0x2

.field public static final CONTENT_TYPE_NONE:I = 0x0

.field public static final CONTENT_TYPE_TEXT:I = 0x1

.field public static final CONTENT_TYPE_URL:I = 0x3

.field private static final CONVERSION_BITMAP:Ljava/lang/String; = "conversionBitmap"

.field public static final PACKAGENAME_UGC:Ljava/lang/String; = "com.ss.android.ugc.aweme"

.field public static final PACKAGENAME_WECHAT:Ljava/lang/String; = "com.tencent.mm"

.field private static final TAG:Ljava/lang/String; = "ViewContentFetcher"

.field public static final VIEW_SIZE_MIN:I = 0x96


# instance fields
.field public mPackageName:Ljava/lang/String;

.field public mVersionCode:I

.field public mViewClassReflectionMap:Ljava/util/Map;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field


# direct methods
.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const-string p0, "ViewContentFetcher"

    invoke-static {p0, p0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    return-void
.end method

.method private fetchDefaultContent(Ljava/lang/String;Landroid/view/View;I)Landroid/os/Bundle;
    .locals 1

    const-string v0, "com.tencent.mm"

    invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_0

    new-instance p1, Ljava/lang/StringBuilder;

    const-string v0, "fetchWechatDefaultContent, is wechat:"

    invoke-direct {p1, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    const-string v0, "ViewContentFetcher"

    invoke-static {v0, p1}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    invoke-direct {p0, p2, p3}, Lcom/miui/catcherpatch/ViewContentFetcher;->fetchWechatDefaultContent(Landroid/view/View;I)Landroid/os/Bundle;

    move-result-object p0

    return-object p0

    :cond_0
    const/4 p0, 0x0

    return-object p0
.end method

.method private final fetchImageViewContent(Landroid/widget/ImageView;)Landroid/os/Bundle;
    .locals 4

    invoke-virtual {p1}, Landroid/widget/ImageView;->getDrawable()Landroid/graphics/drawable/Drawable;

    move-result-object v0

    const/4 v1, 0x0

    if-eqz v0, :cond_2

    const-string v2, "ViewContentFetcher"

    const-string v3, "fetchImageViewContent, drawable is not null"

    invoke-static {v2, v3}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    :try_start_0
    instance-of v3, v0, Landroid/graphics/drawable/BitmapDrawable;

    if-eqz v3, :cond_0

    check-cast v0, Landroid/graphics/drawable/BitmapDrawable;

    invoke-virtual {v0}, Landroid/graphics/drawable/BitmapDrawable;->getBitmap()Landroid/graphics/Bitmap;

    move-result-object v0
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    :cond_0
    move-object v0, v1

    :goto_0
    if-nez v0, :cond_1

    const-string v0, "fetchImageViewContent, bitmap is null, exec getBitmapFromView"

    invoke-static {v2, v0}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {p1}, Ll/a;->a(Landroid/view/View;)Landroid/graphics/Bitmap;

    move-result-object v0

    :cond_1
    if-eqz v0, :cond_2

    new-instance v1, Landroid/os/Bundle;

    invoke-direct {v1}, Landroid/os/Bundle;-><init>()V

    invoke-direct {p0, v1, v0}, Lcom/miui/catcherpatch/ViewContentFetcher;->setBundleResult(Landroid/os/Bundle;Landroid/graphics/Bitmap;)V

    const-string p0, "viewBitmap"

    invoke-virtual {v1, p0, v0}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    const-string p0, "catchType"

    const/4 p1, 0x2

    invoke-virtual {v1, p0, p1}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    :cond_2
    return-object v1
.end method

.method private final fetchNativeViewContent(Landroid/view/View;Z)Landroid/os/Bundle;
    .locals 3

    invoke-virtual {p1}, Landroid/view/View;->createAccessibilityNodeInfo()Landroid/view/accessibility/AccessibilityNodeInfo;

    move-result-object p0

    const-string v0, "ViewContentFetcher"

    if-eqz p0, :cond_0

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "fetchNativeViewContent, AccessibilityNodeInfo hint: "

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Landroid/view/accessibility/AccessibilityNodeInfo;->getHintText()Ljava/lang/CharSequence;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v2, ", text: "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Landroid/view/accessibility/AccessibilityNodeInfo;->getText()Ljava/lang/CharSequence;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v2, ", desc:"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Landroid/view/accessibility/AccessibilityNodeInfo;->getContentDescription()Ljava/lang/CharSequence;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v2, ", needGetHintView:"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1, p2}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v0, v1}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_0

    :cond_0
    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "fetchNativeViewContent, AccessibilityNodeInfo is null, view:"

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v0, v1}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    :goto_0
    const/4 v1, 0x0

    if-eqz p0, :cond_3

    if-nez p2, :cond_1

    invoke-virtual {p0}, Landroid/view/accessibility/AccessibilityNodeInfo;->getHintText()Ljava/lang/CharSequence;

    move-result-object p2

    invoke-static {p2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result p2

    if-nez p2, :cond_1

    return-object v1

    :cond_1
    invoke-virtual {p0}, Landroid/view/accessibility/AccessibilityNodeInfo;->getText()Ljava/lang/CharSequence;

    move-result-object p2

    invoke-static {p2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result p2

    if-nez p2, :cond_2

    invoke-virtual {p0}, Landroid/view/accessibility/AccessibilityNodeInfo;->getText()Ljava/lang/CharSequence;

    move-result-object p0

    goto :goto_1

    :cond_2
    invoke-virtual {p0}, Landroid/view/accessibility/AccessibilityNodeInfo;->getContentDescription()Ljava/lang/CharSequence;

    move-result-object p2

    invoke-static {p2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result p2

    if-nez p2, :cond_3

    invoke-virtual {p0}, Landroid/view/accessibility/AccessibilityNodeInfo;->getContentDescription()Ljava/lang/CharSequence;

    move-result-object p0

    goto :goto_1

    :cond_3
    move-object p0, v1

    :goto_1
    new-instance p2, Ljava/lang/StringBuilder;

    const-string v2, "fetchNativeViewContent, res: "

    invoke-direct {p2, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p2, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v2, ", view: "

    invoke-virtual {p2, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v0, p1}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    if-eqz p0, :cond_4

    new-instance v1, Landroid/os/Bundle;

    invoke-direct {v1}, Landroid/os/Bundle;-><init>()V

    const-string p1, "viewContent"

    invoke-interface {p0}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v1, p1, p0}, Landroid/os/Bundle;->putString(Ljava/lang/String;Ljava/lang/String;)V

    const-string p0, "catchType"

    const/4 p1, 0x1

    invoke-virtual {v1, p0, p1}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    :cond_4
    return-object v1
.end method

.method private fetchTargetViewContent(Landroid/view/View;Ljava/lang/String;IZ)Landroid/os/Bundle;
    .locals 16

    move-object/from16 v0, p0

    move-object/from16 v1, p2

    const-string v2, "conversionBitmap"

    invoke-virtual {v2, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    const-string v3, "catchType"

    const-string v4, "viewBitmap"

    const/4 v5, 0x0

    const/4 v6, 0x2

    if-eqz v2, :cond_1

    invoke-static/range {p1 .. p1}, Ll/a;->a(Landroid/view/View;)Landroid/graphics/Bitmap;

    move-result-object v0

    if-eqz v0, :cond_0

    new-instance v5, Landroid/os/Bundle;

    invoke-direct {v5}, Landroid/os/Bundle;-><init>()V

    invoke-virtual {v5, v4, v0}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    invoke-virtual {v5, v3, v6}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    :cond_0
    return-object v5

    :cond_1
    const-string v2, ","

    invoke-virtual {v1, v2}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v1

    if-eqz v1, :cond_19

    array-length v2, v1

    if-nez v2, :cond_2

    goto/16 :goto_6

    :cond_2
    new-instance v2, Landroid/os/Bundle;

    invoke-direct {v2}, Landroid/os/Bundle;-><init>()V

    const-string v7, "apkVersion"

    iget v8, v0, Lcom/miui/catcherpatch/ViewContentFetcher;->mVersionCode:I

    invoke-virtual {v2, v7, v8}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    const-string v7, "apkName"

    iget-object v8, v0, Lcom/miui/catcherpatch/ViewContentFetcher;->mPackageName:Ljava/lang/String;

    invoke-virtual {v2, v7, v8}, Landroid/os/Bundle;->putString(Ljava/lang/String;Ljava/lang/String;)V

    const-string v7, "apkParamList"

    invoke-virtual {v2, v7, v1}, Landroid/os/Bundle;->putStringArray(Ljava/lang/String;[Ljava/lang/String;)V

    const/4 v7, 0x1

    const/4 v8, 0x0

    move v10, v7

    move v9, v8

    :goto_0
    array-length v11, v1

    if-ge v9, v11, :cond_16

    aget-object v11, v1, v9

    if-nez v11, :cond_3

    goto/16 :goto_5

    :cond_3
    invoke-virtual {v11, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v11

    if-eqz v11, :cond_6

    and-int/lit8 v10, p3, 0x2

    if-nez v10, :cond_4

    return-object v5

    :cond_4
    invoke-static/range {p1 .. p1}, Lcom/miui/catcherpatch/ViewContentFetcher;->isAcceptSizeView(Landroid/view/View;)Z

    move-result v10

    if-nez v10, :cond_5

    return-object v5

    :cond_5
    move v10, v6

    goto/16 :goto_5

    :cond_6
    aget-object v11, v1, v9

    const-string v12, "miui"

    invoke-virtual {v11, v12}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v11

    if-eqz v11, :cond_7

    goto/16 :goto_5

    :cond_7
    aget-object v11, v1, v9

    const-string v12, "/"

    invoke-virtual {v11, v12}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v11

    if-eqz v11, :cond_15

    array-length v12, v11

    if-eqz v12, :cond_15

    array-length v12, v11

    rem-int/2addr v12, v6

    if-eqz v12, :cond_8

    goto/16 :goto_5

    :cond_8
    move-object/from16 v13, p1

    move v12, v8

    :goto_1
    array-length v14, v11

    if-ge v12, v14, :cond_f

    if-eqz v13, :cond_f

    if-ne v10, v6, :cond_b

    add-int/lit8 v14, v12, 0x1

    aget-object v14, v11, v14

    const-string v15, "Bitmap"

    invoke-virtual {v15, v14}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v14

    if-eqz v14, :cond_b

    aget-object v14, v11, v12

    const-class v15, Landroid/graphics/drawable/Drawable;

    invoke-static {v13, v14, v15}, Lp/c;->h(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Class;)Lmiui/util/ObjectReference;

    move-result-object v13

    if-eqz v13, :cond_a

    invoke-virtual {v13}, Lmiui/util/ObjectReference;->get()Ljava/lang/Object;

    move-result-object v14

    if-eqz v14, :cond_a

    invoke-virtual {v13}, Lmiui/util/ObjectReference;->get()Ljava/lang/Object;

    move-result-object v13

    check-cast v13, Landroid/graphics/drawable/Drawable;

    if-nez v13, :cond_9

    goto :goto_2

    :cond_9
    :try_start_0
    instance-of v14, v13, Landroid/graphics/drawable/BitmapDrawable;

    if-eqz v14, :cond_a

    check-cast v13, Landroid/graphics/drawable/BitmapDrawable;

    invoke-virtual {v13}, Landroid/graphics/drawable/BitmapDrawable;->getBitmap()Landroid/graphics/Bitmap;

    move-result-object v13
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_3

    :catch_0
    :cond_a
    :goto_2
    move-object v13, v5

    :goto_3
    if-nez v13, :cond_d

    invoke-virtual/range {p1 .. p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v14

    invoke-virtual {v14}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v14

    const-string v15, "ImageView"

    invoke-virtual {v14, v15}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v14

    if-eqz v14, :cond_d

    invoke-direct/range {p0 .. p1}, Lcom/miui/catcherpatch/ViewContentFetcher;->fetchViewBitmap(Landroid/view/View;)Landroid/os/Bundle;

    move-result-object v0

    return-object v0

    :cond_b
    aget-object v14, v11, v12

    const-class v15, Ljava/lang/Object;

    invoke-static {v13, v14, v15}, Lp/c;->h(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Class;)Lmiui/util/ObjectReference;

    move-result-object v13

    if-eqz v13, :cond_e

    invoke-virtual {v13}, Lmiui/util/ObjectReference;->get()Ljava/lang/Object;

    move-result-object v14

    if-nez v14, :cond_c

    goto :goto_4

    :cond_c
    invoke-virtual {v13}, Lmiui/util/ObjectReference;->get()Ljava/lang/Object;

    move-result-object v13

    :cond_d
    add-int/lit8 v12, v12, 0x2

    goto :goto_1

    :cond_e
    :goto_4
    move-object v13, v5

    :cond_f
    if-ne v10, v6, :cond_10

    instance-of v11, v13, Landroid/graphics/Bitmap;

    if-eqz v11, :cond_10

    check-cast v13, Landroid/os/Parcelable;

    invoke-virtual {v2, v4, v13}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    goto :goto_5

    :cond_10
    instance-of v11, v13, Ljava/lang/CharSequence;

    if-eqz v11, :cond_11

    aget-object v11, v1, v9

    check-cast v13, Ljava/lang/CharSequence;

    invoke-virtual {v2, v11, v13}, Landroid/os/Bundle;->putCharSequence(Ljava/lang/String;Ljava/lang/CharSequence;)V

    goto :goto_5

    :cond_11
    instance-of v11, v13, Ljava/io/Serializable;

    if-eqz v11, :cond_12

    aget-object v11, v1, v9

    check-cast v13, Ljava/io/Serializable;

    invoke-virtual {v2, v11, v13}, Landroid/os/Bundle;->putSerializable(Ljava/lang/String;Ljava/io/Serializable;)V

    goto :goto_5

    :cond_12
    instance-of v11, v13, Landroid/os/Parcelable;

    if-eqz v11, :cond_13

    aget-object v11, v1, v9

    check-cast v13, Landroid/os/Parcelable;

    invoke-virtual {v2, v11, v13}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    goto :goto_5

    :cond_13
    instance-of v11, v13, Ljava/util/ArrayList;

    if-eqz v11, :cond_15

    check-cast v13, Ljava/util/ArrayList;

    invoke-virtual {v13}, Ljava/util/ArrayList;->size()I

    move-result v11

    if-lez v11, :cond_15

    invoke-virtual {v13, v8}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    move-result-object v11

    instance-of v11, v11, Landroid/os/Parcelable;

    if-eqz v11, :cond_14

    aget-object v11, v1, v9

    invoke-virtual {v2, v11, v13}, Landroid/os/Bundle;->putParcelableArrayList(Ljava/lang/String;Ljava/util/ArrayList;)V

    goto :goto_5

    :cond_14
    invoke-virtual {v13, v8}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    move-result-object v11

    instance-of v11, v11, Ljava/lang/CharSequence;

    if-eqz v11, :cond_15

    aget-object v11, v1, v9

    invoke-virtual {v2, v11, v13}, Landroid/os/Bundle;->putCharSequenceArrayList(Ljava/lang/String;Ljava/util/ArrayList;)V

    :cond_15
    :goto_5
    add-int/lit8 v9, v9, 0x1

    goto/16 :goto_0

    :cond_16
    invoke-virtual {v2, v3, v10}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    and-int/lit8 v1, p3, 0x1

    if-eqz v1, :cond_17

    move-object/from16 v1, p1

    move/from16 v3, p4

    invoke-direct {v0, v1, v3}, Lcom/miui/catcherpatch/ViewContentFetcher;->fetchNativeViewContent(Landroid/view/View;Z)Landroid/os/Bundle;

    move-result-object v0

    return-object v0

    :cond_17
    sget-boolean v0, Lp/b;->a:Z

    if-eqz v0, :cond_18

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "res "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "ViewContentFetcher"

    invoke-static {v1, v0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    :cond_18
    return-object v2

    :cond_19
    :goto_6
    return-object v5
.end method

.method private final fetchViewBitmap(Landroid/view/View;)Landroid/os/Bundle;
    .locals 1

    invoke-static {p1}, Ll/a;->a(Landroid/view/View;)Landroid/graphics/Bitmap;

    move-result-object p1

    if-eqz p1, :cond_0

    new-instance v0, Landroid/os/Bundle;

    invoke-direct {v0}, Landroid/os/Bundle;-><init>()V

    invoke-direct {p0, v0, p1}, Lcom/miui/catcherpatch/ViewContentFetcher;->setBundleResult(Landroid/os/Bundle;Landroid/graphics/Bitmap;)V

    const-string p0, "viewBitmap"

    invoke-virtual {v0, p0, p1}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    const-string p0, "catchType"

    const/4 p1, 0x2

    invoke-virtual {v0, p0, p1}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    goto :goto_0

    :cond_0
    const/4 v0, 0x0

    :goto_0
    return-object v0
.end method

.method private fetchWechatDefaultContent(Landroid/view/View;I)Landroid/os/Bundle;
    .locals 8

    const-string p0, "mText"

    const-class v0, Ljava/lang/CharSequence;

    invoke-static {p1, p0, v0}, Lp/c;->h(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Class;)Lmiui/util/ObjectReference;

    move-result-object p0

    const/4 v1, 0x1

    const/4 v2, 0x0

    if-nez p0, :cond_2

    const-string p0, "ReflectionUtils"

    :try_start_0
    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/Class;->getFields()[Ljava/lang/reflect/Field;

    move-result-object v3

    array-length v4, v3

    const/4 v5, 0x0

    :goto_0
    if-ge v5, v4, :cond_1

    aget-object v6, v3, v5

    invoke-virtual {v6, v1}, Ljava/lang/reflect/AccessibleObject;->setAccessible(Z)V

    invoke-virtual {v6}, Ljava/lang/reflect/Field;->getType()Ljava/lang/Class;

    move-result-object v7

    if-ne v7, v0, :cond_0

    invoke-virtual {v6, p1}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "tryGetObjectFieldByTypeClass fieldValue: "

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {p0, v3}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    if-eqz v0, :cond_1

    new-instance v3, Lmiui/util/ObjectReference;

    invoke-direct {v3, v0}, Lmiui/util/ObjectReference;-><init>(Ljava/lang/Object;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-object p0, v3

    goto :goto_2

    :catch_0
    move-exception v0

    goto :goto_1

    :cond_0
    add-int/lit8 v5, v5, 0x1

    goto :goto_0

    :goto_1
    const-string v3, "tryGetObjectFieldByTypeClass error"

    invoke-static {p0, v3, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    :cond_1
    move-object p0, v2

    :cond_2
    :goto_2
    const-string v0, "catchType"

    if-eqz p0, :cond_3

    invoke-virtual {p0}, Lmiui/util/ObjectReference;->get()Ljava/lang/Object;

    move-result-object v3

    if-eqz v3, :cond_3

    invoke-virtual {p0}, Lmiui/util/ObjectReference;->get()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/CharSequence;

    invoke-interface {v3}, Ljava/lang/CharSequence;->length()I

    move-result v3

    if-lez v3, :cond_3

    new-instance v2, Landroid/os/Bundle;

    invoke-direct {v2}, Landroid/os/Bundle;-><init>()V

    invoke-virtual {p0}, Lmiui/util/ObjectReference;->get()Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Ljava/lang/CharSequence;

    invoke-interface {p0}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object p0

    new-instance p2, Ljava/lang/StringBuilder;

    const-string v3, "fetchWechatDefaultContent, content:"

    invoke-direct {p2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p2, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v3, ", view:"

    invoke-virtual {p2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    const-string p2, "ViewContentFetcher"

    invoke-static {p2, p1}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    const-string p1, "viewContent"

    invoke-virtual {v2, p1, p0}, Landroid/os/Bundle;->putString(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v2, v0, v1}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    goto :goto_5

    :cond_3
    const/4 p0, 0x2

    and-int/2addr p2, p0

    if-eqz p2, :cond_6

    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object p2

    invoke-virtual {p2}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object p2

    const-string v1, "Image"

    invoke-virtual {p2, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result p2

    if-eqz p2, :cond_6

    invoke-static {p1}, Lcom/miui/catcherpatch/ViewContentFetcher;->isAcceptSizeView(Landroid/view/View;)Z

    move-result p2

    if-eqz p2, :cond_6

    const-class p2, Landroid/graphics/drawable/Drawable;

    const-string v1, "mDrawable"

    invoke-static {p1, v1, p2}, Lp/c;->h(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Class;)Lmiui/util/ObjectReference;

    move-result-object p1

    if-eqz p1, :cond_5

    invoke-virtual {p1}, Lmiui/util/ObjectReference;->get()Ljava/lang/Object;

    move-result-object p2

    if-eqz p2, :cond_5

    invoke-virtual {p1}, Lmiui/util/ObjectReference;->get()Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Landroid/graphics/drawable/Drawable;

    if-nez p1, :cond_4

    goto :goto_3

    :cond_4
    :try_start_1
    instance-of p2, p1, Landroid/graphics/drawable/BitmapDrawable;

    if-eqz p2, :cond_5

    check-cast p1, Landroid/graphics/drawable/BitmapDrawable;

    invoke-virtual {p1}, Landroid/graphics/drawable/BitmapDrawable;->getBitmap()Landroid/graphics/Bitmap;

    move-result-object p1
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1

    goto :goto_4

    :catch_1
    :cond_5
    :goto_3
    move-object p1, v2

    :goto_4
    if-eqz p1, :cond_6

    new-instance v2, Landroid/os/Bundle;

    invoke-direct {v2}, Landroid/os/Bundle;-><init>()V

    const-string p2, "viewBitmap"

    invoke-virtual {v2, p2, p1}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    invoke-virtual {v2, v0, p0}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    :cond_6
    :goto_5
    return-object v2
.end method

.method private static isAcceptSizeView(Landroid/view/View;)Z
    .locals 3

    const/4 v0, 0x0

    :try_start_0
    invoke-virtual {p0}, Landroid/view/View;->getWidth()I

    move-result v1

    const/16 v2, 0x96

    if-lt v1, v2, :cond_1

    invoke-virtual {p0}, Landroid/view/View;->getHeight()I

    move-result p0
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    if-ge p0, v2, :cond_0

    goto :goto_0

    :cond_0
    const/4 p0, 0x1

    return p0

    :catch_0
    move-exception p0

    goto :goto_1

    :cond_1
    :goto_0
    return v0

    :goto_1
    const-string v1, "isAcceptSizeView-Exception: "

    const-string v2, "ViewContentFetcher"

    invoke-static {v1, p0, v2}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    return v0
.end method

.method private isPureBackgroundColor(Landroid/graphics/Bitmap;)Z
    .locals 5

    const/4 p0, 0x1

    const/4 v0, 0x0

    move v1, v0

    :goto_0
    const/16 v2, 0xf

    if-ge v1, v2, :cond_3

    move v3, v0

    :goto_1
    if-ge v3, v2, :cond_1

    invoke-virtual {p1, v1, v3}, Landroid/graphics/Bitmap;->getPixel(II)I

    move-result v4

    if-eqz v4, :cond_0

    move p0, v0

    goto :goto_2

    :cond_0
    add-int/lit8 v3, v3, 0x1

    goto :goto_1

    :cond_1
    :goto_2
    if-nez p0, :cond_2

    goto :goto_3

    :cond_2
    add-int/lit8 v1, v1, 0x1

    goto :goto_0

    :cond_3
    :goto_3
    return p0
.end method

.method private setBundleResult(Landroid/os/Bundle;Landroid/graphics/Bitmap;)V
    .locals 1

    const-string v0, "isPureColor"

    invoke-direct {p0, p2}, Lcom/miui/catcherpatch/ViewContentFetcher;->isPureBackgroundColor(Landroid/graphics/Bitmap;)Z

    move-result p0

    invoke-virtual {p1, v0, p0}, Landroid/os/Bundle;->putBoolean(Ljava/lang/String;Z)V

    return-void
.end method


# virtual methods
.method public createFetcher(Lmiui/contentcatcher/sdk/Token;Ljava/util/Map;)V
    .locals 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lmiui/contentcatcher/sdk/Token;",
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;)V"
        }
    .end annotation

    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/Token;->getPkgName()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/catcherpatch/ViewContentFetcher;->mPackageName:Ljava/lang/String;

    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/Token;->getVersionCode()I

    move-result p1

    iput p1, p0, Lcom/miui/catcherpatch/ViewContentFetcher;->mVersionCode:I

    iput-object p2, p0, Lcom/miui/catcherpatch/ViewContentFetcher;->mViewClassReflectionMap:Ljava/util/Map;

    return-void
.end method

.method public fetchViewContent(Landroid/view/View;I)Landroid/os/Bundle;
    .locals 1

    const/4 v0, 0x0

    .line 1
    invoke-virtual {p0, p1, p2, v0}, Lcom/miui/catcherpatch/ViewContentFetcher;->fetchViewContent(Landroid/view/View;IZ)Landroid/os/Bundle;

    move-result-object p0

    return-object p0
.end method

.method public fetchViewContentImpl(Landroid/view/View;IZ)Landroid/os/Bundle;
    .locals 6

    .line 2
    const-string v0, "ViewContentFetcher : "

    const-string v1, "5 fetchNativeViewContent: "

    const/4 v2, 0x0

    if-nez p1, :cond_0

    return-object v2

    .line 3
    :cond_0
    :try_start_0
    iget-object v3, p0, Lcom/miui/catcherpatch/ViewContentFetcher;->mViewClassReflectionMap:Ljava/util/Map;

    if-eqz v3, :cond_1

    .line 4
    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v4

    invoke-interface {v3, v4}, Ljava/util/Map;->containsKey(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_1

    .line 5
    iget-object v3, p0, Lcom/miui/catcherpatch/ViewContentFetcher;->mViewClassReflectionMap:Ljava/util/Map;

    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v4

    invoke-interface {v3, v4}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/String;

    goto :goto_0

    :catch_0
    move-exception p0

    goto/16 :goto_3

    :cond_1
    move-object v3, v2

    :goto_0
    if-eqz v3, :cond_2

    .line 6
    invoke-direct {p0, p1, v3, p2, p3}, Lcom/miui/catcherpatch/ViewContentFetcher;->fetchTargetViewContent(Landroid/view/View;Ljava/lang/String;IZ)Landroid/os/Bundle;

    move-result-object v2

    goto :goto_1

    .line 7
    :cond_2
    iget-object v3, p0, Lcom/miui/catcherpatch/ViewContentFetcher;->mPackageName:Ljava/lang/String;

    invoke-direct {p0, v3, p1, p2}, Lcom/miui/catcherpatch/ViewContentFetcher;->fetchDefaultContent(Ljava/lang/String;Landroid/view/View;I)Landroid/os/Bundle;

    move-result-object v2

    :goto_1
    if-nez v2, :cond_8

    and-int/lit8 v3, p2, 0x2

    const/4 v4, 0x1

    if-eqz v3, :cond_3

    .line 8
    invoke-static {p1}, Lcom/miui/catcherpatch/ViewContentFetcher;->isAcceptSizeView(Landroid/view/View;)Z

    move-result v3

    if-eqz v3, :cond_3

    move v3, v4

    goto :goto_2

    :cond_3
    const/4 v3, 0x0

    .line 9
    :goto_2
    instance-of v5, p1, Landroid/widget/ImageView;

    if-eqz v5, :cond_4

    if-eqz v3, :cond_4

    .line 10
    check-cast p1, Landroid/widget/ImageView;

    invoke-direct {p0, p1}, Lcom/miui/catcherpatch/ViewContentFetcher;->fetchImageViewContent(Landroid/widget/ImageView;)Landroid/os/Bundle;

    move-result-object p0

    return-object p0

    :cond_4
    if-eqz v3, :cond_6

    .line 11
    sget-object v3, Lv/c;->a:Ljava/util/HashSet;

    .line 12
    new-instance v3, Ljava/util/HashSet;

    invoke-direct {v3}, Ljava/util/HashSet;-><init>()V

    .line 13
    const-string v5, "com.ss.android.lark.imagewidget.largeimage.LargeImageView"

    invoke-virtual {v3, v5}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    .line 14
    const-string v5, "com.miui.newhome.view.GestureImage"

    invoke-virtual {v3, v5}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    .line 15
    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v3, v5}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_5

    .line 16
    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v3

    const-string v5, "ImageView"

    invoke-virtual {v3, v5}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v3

    if-eqz v3, :cond_6

    .line 17
    :cond_5
    invoke-direct {p0, p1}, Lcom/miui/catcherpatch/ViewContentFetcher;->fetchViewBitmap(Landroid/view/View;)Landroid/os/Bundle;

    move-result-object p0

    return-object p0

    :cond_6
    and-int/2addr p2, v4

    if-eqz p2, :cond_8

    .line 18
    new-instance p2, Ljava/lang/StringBuilder;

    invoke-direct {p2, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    .line 19
    sget-boolean v1, Lp/b;->a:Z

    if-eqz v1, :cond_7

    .line 20
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    const-string v0, "ContentCatcher_collect"

    invoke-static {v0, p2}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    .line 21
    :cond_7
    invoke-direct {p0, p1, p3}, Lcom/miui/catcherpatch/ViewContentFetcher;->fetchNativeViewContent(Landroid/view/View;Z)Landroid/os/Bundle;

    move-result-object p0
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return-object p0

    .line 22
    :goto_3
    const-string p1, "fetchTargetViewContent-Exception: "

    const-string p2, "ViewContentFetcher"

    .line 23
    invoke-static {p1, p0, p2}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_8
    return-object v2
.end method

.method public final fetchWebViewBitmap(Landroid/view/View;I)Landroid/os/Bundle;
    .locals 2

    const/4 p0, 0x0

    if-nez p1, :cond_0

    return-object p0

    :cond_0
    const/4 v0, 0x2

    and-int/2addr p2, v0

    if-eqz p2, :cond_2

    :try_start_0
    invoke-static {p1}, Ll/a;->b(Landroid/view/View;)Landroid/graphics/Bitmap;

    move-result-object p1

    if-eqz p1, :cond_1

    new-instance p2, Landroid/os/Bundle;

    invoke-direct {p2}, Landroid/os/Bundle;-><init>()V

    const-string v1, "viewBitmap"

    invoke-virtual {p2, v1, p1}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    const-string p1, "catchType"

    invoke-virtual {p2, p1, v0}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-object p0, p2

    goto :goto_0

    :catch_0
    move-exception p1

    goto :goto_1

    :cond_1
    :goto_0
    return-object p0

    :goto_1
    const-string p2, "fetchWebViewBitmap-Exception: "

    const-string v0, "ViewContentFetcher"

    invoke-static {p2, p1, v0}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_2
    return-object p0
.end method

.method public reservedFunction(Ljava/util/Map;I)Ljava/util/Map;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/Object;",
            ">;I)",
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/Object;",
            ">;"
        }
    .end annotation

    const/4 p0, 0x0

    return-object p0
.end method

.method public fetchViewContent(Landroid/view/View;IZ)Landroid/os/Bundle;
    .locals 3

    invoke-static {p1, p2, p3}, Ldev/dragshare/catcher/Bridge;->preExtract(Landroid/view/View;IZ)Landroid/os/Bundle;

    move-result-object v0

    if-eqz v0, :orig

    return-object v0

    :orig
    invoke-virtual {p0, p1, p2, p3}, Lcom/miui/catcherpatch/ViewContentFetcher;->fetchViewContentImpl(Landroid/view/View;IZ)Landroid/os/Bundle;

    move-result-object v0

    invoke-static {p1, p2, p3, v0}, Ldev/dragshare/catcher/Bridge;->enhanceFetchViewContent(Landroid/view/View;IZLandroid/os/Bundle;)Landroid/os/Bundle;

    move-result-object v1

    return-object v1
.end method
