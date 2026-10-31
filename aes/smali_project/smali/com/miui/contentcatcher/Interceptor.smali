.class public Lcom/miui/contentcatcher/Interceptor;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lmiui/contentcatcher/IInterceptor;
.implements Lq/b;


# annotations
.annotation build Landroid/annotation/TargetApi;
    value = 0x3
.end annotation

.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/miui/contentcatcher/Interceptor$e;,
        Lcom/miui/contentcatcher/Interceptor$d;
    }
.end annotation


# instance fields
.field public final a:Landroid/app/Activity;

.field public final b:Ljava/lang/String;

.field public final c:Lmiui/contentcatcher/sdk/Token;

.field public d:Lu/a;

.field public final e:Ld/c;

.field public f:Lq/a;

.field public g:Z

.field public final h:Ljava/lang/String;

.field public i:Z

.field public j:Z

.field public volatile k:Z

.field public l:Z

.field public m:Z

.field public n:Z

.field public final o:Ljava/util/LinkedList;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/LinkedList<",
            "Ljava/lang/ref/WeakReference<",
            "Landroid/view/View;",
            ">;>;"
        }
    .end annotation
.end field

.field public p:Landroid/view/View;

.field public q:I

.field public final r:Lmiui/contentcatcher/IViewContentFetcher;

.field public s:Lmiui/contentcatcher/sdk/Content;

.field public final t:Landroid/app/AppOpsManager;

.field public u:Ly/g;

.field public v:Lz/j;

.field public final w:Lcom/miui/contentcatcher/Interceptor$a;

.field public final x:Landroid/os/Handler;

.field public final y:Landroid/os/Handler;

.field public volatile z:Landroid/os/Handler;


# direct methods
.method public constructor <init>(Landroid/app/Activity;)V
    .locals 10

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const/4 v0, 0x0

    iput-boolean v0, p0, Lcom/miui/contentcatcher/Interceptor;->i:Z

    iput-boolean v0, p0, Lcom/miui/contentcatcher/Interceptor;->j:Z

    iput-boolean v0, p0, Lcom/miui/contentcatcher/Interceptor;->k:Z

    iput-boolean v0, p0, Lcom/miui/contentcatcher/Interceptor;->l:Z

    iput-boolean v0, p0, Lcom/miui/contentcatcher/Interceptor;->m:Z

    iput-boolean v0, p0, Lcom/miui/contentcatcher/Interceptor;->n:Z

    new-instance v1, Ljava/util/LinkedList;

    invoke-direct {v1}, Ljava/util/LinkedList;-><init>()V

    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->o:Ljava/util/LinkedList;

    const/4 v1, 0x0

    iput v0, p0, Lcom/miui/contentcatcher/Interceptor;->q:I

    new-instance v2, Ly/g;

    invoke-direct {v2}, Ly/g;-><init>()V

    iput-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->u:Ly/g;

    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->v:Lz/j;

    new-instance v2, Lcom/miui/contentcatcher/Interceptor$a;

    invoke-direct {v2, p0}, Lcom/miui/contentcatcher/Interceptor$a;-><init>(Lcom/miui/contentcatcher/Interceptor;)V

    iput-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->w:Lcom/miui/contentcatcher/Interceptor$a;

    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->z:Landroid/os/Handler;

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "Init Interceptor for Activity "

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    const-string v3, "Interceptor_Catcher"

    invoke-static {v3, v2}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    iput-object p1, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    invoke-static {p1}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I

    move-result v2

    invoke-static {v2}, Ljava/lang/Integer;->toHexString(I)Ljava/lang/String;

    move-result-object v2

    iput-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->h:Ljava/lang/String;

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->i()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->g()Ljava/lang/String;

    move-result-object v5

    new-instance v6, Lmiui/contentcatcher/sdk/Token;

    const/4 v7, -0x1

    if-nez p1, :cond_0

    goto :goto_0

    :cond_0
    :try_start_0
    invoke-virtual {p1}, Landroid/app/Activity;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object p1

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->i()Ljava/lang/String;

    move-result-object v8

    invoke-virtual {p1, v8, v0}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object p1

    iget v7, p1, Landroid/content/pm/PackageInfo;->versionCode:I
    :try_end_0
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p1

    new-instance v8, Ljava/lang/StringBuilder;

    const-string v9, "NameNotFoundException: "

    invoke-direct {v8, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v8, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v3, p1}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    :goto_0
    invoke-direct {v6, v4, v5, v2, v7}, Lmiui/contentcatcher/sdk/Token;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V

    iput-object v6, p0, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;

    invoke-static {v4}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result p1

    if-nez p1, :cond_2

    invoke-static {v5}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result p1

    if-nez p1, :cond_2

    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p1, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v2, "@"

    invoke-virtual {p1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    if-nez v2, :cond_1

    goto :goto_1

    :cond_1
    invoke-virtual {v2}, Ljava/lang/Object;->hashCode()I

    move-result v0

    :goto_1
    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    goto :goto_2

    :cond_2
    move-object p1, v1

    :goto_2
    iput-object p1, p0, Lcom/miui/contentcatcher/Interceptor;->b:Ljava/lang/String;

    new-instance p1, Ld/c;

    invoke-direct {p1}, Ljava/lang/Object;-><init>()V

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    iput-object v0, p1, Ld/c;->a:Ljava/io/Serializable;

    iput-object p1, p0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    invoke-static {}, Lmiui/contentcatcher/InterceptorProxy;->getWorkThread()Landroid/os/HandlerThread;

    move-result-object p1

    invoke-virtual {p1}, Landroid/os/HandlerThread;->getLooper()Landroid/os/Looper;

    move-result-object p1

    new-instance v0, Landroid/os/Handler;

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->w:Lcom/miui/contentcatcher/Interceptor$a;

    invoke-direct {v0, p1, v2}, Landroid/os/Handler;-><init>(Landroid/os/Looper;Landroid/os/Handler$Callback;)V

    iput-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->x:Landroid/os/Handler;

    iput-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->y:Landroid/os/Handler;

    const-string p1, "ViewContentFactory"

    const-string v0, "createInterceptor took "

    const/4 v2, 0x1

    :try_start_1
    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v4

    invoke-static {}, Lm/c;->b()Ljava/lang/Class;

    move-result-object v6

    if-nez v6, :cond_3

    :goto_3
    move-object v6, v1

    goto/16 :goto_a

    :cond_3
    invoke-virtual {v6, v1}, Ljava/lang/Class;->getConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;

    move-result-object v6

    if-nez v6, :cond_4

    goto :goto_3

    :cond_4
    invoke-virtual {v6}, Ljava/lang/reflect/AccessibleObject;->isAccessible()Z

    move-result v7

    if-nez v7, :cond_5

    invoke-virtual {v6, v2}, Ljava/lang/reflect/AccessibleObject;->setAccessible(Z)V

    goto :goto_4

    :catch_1
    move-exception v0

    move-object v6, v1

    goto :goto_5

    :catch_2
    move-exception v0

    move-object v6, v1

    goto :goto_6

    :catch_3
    move-exception v0

    move-object v6, v1

    goto :goto_7

    :catch_4
    move-exception v0

    move-object v6, v1

    goto :goto_8

    :catch_5
    move-exception v0

    move-object v6, v1

    goto :goto_9

    :cond_5
    :goto_4
    invoke-virtual {v6, v1}, Ljava/lang/reflect/Constructor;->newInstance([Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Lmiui/contentcatcher/IViewContentFetcher;
    :try_end_1
    .catch Ljava/lang/InstantiationException; {:try_start_1 .. :try_end_1} :catch_5
    .catch Ljava/lang/IllegalAccessException; {:try_start_1 .. :try_end_1} :catch_4
    .catch Ljava/lang/reflect/InvocationTargetException; {:try_start_1 .. :try_end_1} :catch_3
    .catch Ljava/lang/NoSuchMethodException; {:try_start_1 .. :try_end_1} :catch_2
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1

    :try_start_2
    new-instance v7, Ljava/lang/StringBuilder;

    invoke-direct {v7, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v8

    sub-long/2addr v8, v4

    invoke-virtual {v7, v8, v9}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    const-string v0, "ms"

    invoke-virtual {v7, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {p1, v0}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_2
    .catch Ljava/lang/InstantiationException; {:try_start_2 .. :try_end_2} :catch_a
    .catch Ljava/lang/IllegalAccessException; {:try_start_2 .. :try_end_2} :catch_9
    .catch Ljava/lang/reflect/InvocationTargetException; {:try_start_2 .. :try_end_2} :catch_8
    .catch Ljava/lang/NoSuchMethodException; {:try_start_2 .. :try_end_2} :catch_7
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_6

    goto :goto_a

    :catch_6
    move-exception v0

    goto :goto_5

    :catch_7
    move-exception v0

    goto :goto_6

    :catch_8
    move-exception v0

    goto :goto_7

    :catch_9
    move-exception v0

    goto :goto_8

    :catch_a
    move-exception v0

    goto :goto_9

    :goto_5
    const-string v4, "Exception"

    invoke-static {p1, v4, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    goto :goto_a

    :goto_6
    const-string v4, "NoSuchMethodException"

    invoke-static {p1, v4, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    goto :goto_a

    :goto_7
    const-string v4, "InvocationTargetException"

    invoke-static {p1, v4, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    goto :goto_a

    :goto_8
    const-string v4, "IllegalAccessException"

    invoke-static {p1, v4, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    goto :goto_a

    :goto_9
    const-string v4, "InstantiationException"

    invoke-static {p1, v4, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    :goto_a
    iput-object v6, p0, Lcom/miui/contentcatcher/Interceptor;->r:Lmiui/contentcatcher/IViewContentFetcher;

    if-nez v6, :cond_6

    const-string p1, "mViewContentFetcher is NULL"

    invoke-static {v3, p1}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    :cond_6
    iget-object p1, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    if-eqz p1, :cond_7

    invoke-virtual {p1}, Landroid/app/Activity;->getApplicationContext()Landroid/content/Context;

    move-result-object p1

    const-string v0, "appops"

    invoke-virtual {p1, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Landroid/app/AppOpsManager;

    iput-object p1, p0, Lcom/miui/contentcatcher/Interceptor;->t:Landroid/app/AppOpsManager;

    :cond_7
    iget-object p1, p0, Lcom/miui/contentcatcher/Interceptor;->u:Ly/g;

    if-eqz p1, :cond_8

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    iput-object p0, p1, Ly/g;->a:Lcom/miui/contentcatcher/Interceptor;

    iput-object v0, p1, Ly/g;->b:Landroid/content/Context;

    const-class p0, Lmiui/contentcatcher/InterceptorProxy;

    const-string p1, "mContentExtensionEnabled"

    sget-object v0, Lp/c;->a:Ljava/util/HashMap;

    :try_start_3
    invoke-virtual {p0, p1}, Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;

    move-result-object p0

    invoke-virtual {p0, v2}, Ljava/lang/reflect/AccessibleObject;->setAccessible(Z)V

    invoke-virtual {p0, v1}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_b

    goto :goto_b

    :catch_b
    move-exception p0

    const-string p1, "getStaticObjectField error"

    const-string v0, "SettingTrigger"

    invoke-static {v0, p1, p0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    :cond_8
    :goto_b
    return-void
.end method


# virtual methods
.method public final a(Landroid/view/View;Ljava/util/ArrayList;I)V
    .locals 6

    const-string v0, "Interceptor_Catcher"

    if-nez p1, :cond_0

    const-string p0, "view is null"

    invoke-static {v0, p0}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    return-void

    :cond_0
    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->r:Lmiui/contentcatcher/IViewContentFetcher;

    if-eqz p0, :cond_1

    :try_start_0
    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v1

    const-string v2, "fetchViewContent"

    const-class v3, Landroid/view/View;

    sget-object v4, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    sget-object v5, Ljava/lang/Boolean;->TYPE:Ljava/lang/Class;

    filled-new-array {v3, v4, v5}, [Ljava/lang/Class;

    move-result-object v3

    invoke-virtual {v1, v2, v3}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v1

    invoke-static {p3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p3

    sget-object v2, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    filled-new-array {p1, p3, v2}, [Ljava/lang/Object;

    move-result-object p3

    invoke-virtual {v1, p0, p3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Landroid/os/Bundle;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p0

    invoke-virtual {p0}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_1
    const/4 p0, 0x0

    :goto_0
    if-eqz p0, :cond_3

    const-string p3, "isPureColor"

    invoke-virtual {p0, p3}, Landroid/os/Bundle;->getBoolean(Ljava/lang/String;)Z

    move-result p3

    if-eqz p3, :cond_2

    const-string p3, "screenshot due pure or white-black background"

    invoke-static {v0, p3}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p3, Lcom/miui/contentcatcher/Interceptor$d;

    invoke-direct {p3, p1, p0}, Lcom/miui/contentcatcher/Interceptor$d;-><init>(Landroid/view/View;Landroid/os/Bundle;)V

    invoke-virtual {p2, p3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    return-void

    :cond_2
    const-string p3, "view_rect"

    invoke-static {p1}, Lv/c;->c(Landroid/view/View;)Landroid/graphics/Rect;

    move-result-object v0

    invoke-virtual {p0, p3, v0}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    new-instance p3, Lcom/miui/contentcatcher/Interceptor$d;

    invoke-direct {p3, p1, p0}, Lcom/miui/contentcatcher/Interceptor$d;-><init>(Landroid/view/View;Landroid/os/Bundle;)V

    invoke-virtual {p2, p3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_3
    return-void
.end method

.method public final b()V
    .locals 6

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    if-eqz v0, :cond_0

    invoke-virtual {v0}, Ld/c;->g()V

    :cond_0
    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->d:Lu/a;

    const/4 v1, 0x0

    if-eqz v0, :cond_1

    iget-object v0, v0, Lu/a;->g:Lu/a$a;

    invoke-virtual {v0, v1}, Landroid/os/Handler;->removeCallbacksAndMessages(Ljava/lang/Object;)V

    :cond_1
    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->v:Lz/j;

    if-eqz v0, :cond_4

    sget v2, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v3, 0x22

    if-lt v2, v3, :cond_3

    const/4 v2, 0x1

    iput-boolean v2, v0, Lz/j;->b:Z

    iget-object v2, v0, Lz/j;->a:Landroid/os/Handler;

    invoke-virtual {v2, v1}, Landroid/os/Handler;->removeCallbacksAndMessages(Ljava/lang/Object;)V

    :goto_0
    iget-object v2, v0, Lz/j;->c:Ljava/util/ArrayDeque;

    invoke-virtual {v2}, Ljava/util/ArrayDeque;->pollFirst()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lz/j$c;

    if-eqz v2, :cond_2

    iget-object v3, v2, Lz/j$c;->a:Ljava/lang/String;

    iget-object v4, v2, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object v2, v2, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v5, "activity destroyed"

    invoke-static {v5, v3, v4, v2}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    goto :goto_0

    :cond_2
    sget-object v0, Lz/j;->d:Lz/j$b;

    sget-object v2, Lz/j$b;->b:Lz/j$b;

    if-ne v0, v2, :cond_3

    sget-object v0, Lz/j$b;->a:Lz/j$b;

    sput-object v0, Lz/j;->d:Lz/j$b;

    :cond_3
    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->v:Lz/j;

    :cond_4
    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->y:Landroid/os/Handler;

    if-eqz v0, :cond_5

    invoke-virtual {v0, v1}, Landroid/os/Handler;->removeCallbacksAndMessages(Ljava/lang/Object;)V

    :cond_5
    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->z:Landroid/os/Handler;

    if-eqz v0, :cond_6

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->z:Landroid/os/Handler;

    invoke-virtual {v0, v1}, Landroid/os/Handler;->removeCallbacksAndMessages(Ljava/lang/Object;)V

    :cond_6
    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->f:Lq/a;

    if-eqz v0, :cond_7

    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->f:Lq/a;

    :cond_7
    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->u:Ly/g;

    if-eqz v0, :cond_8

    iget-object v2, v0, Ly/g;->d:Landroid/os/Handler;

    iget-object v0, v0, Ly/g;->h:Ly/g$e;

    invoke-virtual {v2, v0}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->u:Ly/g;

    :cond_8
    return-void
.end method

.method public final c()V
    .locals 3

    const-string v0, "Interceptor_Catcher"

    const-string v1, "prepare fetchWebViewText"

    invoke-static {v0, v1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    if-nez v1, :cond_0

    const-string p0, "prepare fetchWebViewText mTouchWebView = null"

    invoke-static {v0, p0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    return-void

    :cond_0
    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->u:Ly/g;

    if-eqz v0, :cond_1

    invoke-static {v1}, Ld/a;->c(Landroid/view/View;)Ls/h;

    move-result-object v0

    if-eqz v0, :cond_1

    new-instance v1, Lcom/miui/contentcatcher/Interceptor$c;

    invoke-direct {v1, p0}, Lcom/miui/contentcatcher/Interceptor$c;-><init>(Lcom/miui/contentcatcher/Interceptor;)V

    const-string p0, "WebViewWrapperBase"

    const-string v2, "fetchWebViewText prepare exec js"

    invoke-static {p0, v2}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p0, Ls/g;

    const/4 v2, 0x0

    invoke-direct {p0, v1, v2}, Ls/g;-><init>(Landroid/webkit/ValueCallback;I)V

    const-string v1, "function fetchText() {\n                var array = [];\n                array.push(document.body.innerText);\n                const innerText = document.body.innerText; console.log(innerText);\n                return array;\n        }javascript:fetchText();"

    invoke-virtual {v0, v1, p0}, Ls/h;->b(Ljava/lang/String;Landroid/webkit/ValueCallback;)V

    :cond_1
    return-void
.end method

.method public final d(Landroid/view/View;Landroid/graphics/PointF;Landroid/graphics/PointF;Ljava/util/ArrayList;Ljava/util/HashMap;)V
    .locals 14
    .annotation build Landroid/annotation/TargetApi;
        value = 0xd
    .end annotation

    move-object v7, p0

    move-object v8, p1

    move-object/from16 v9, p2

    move-object/from16 v10, p4

    iget v0, v9, Landroid/graphics/PointF;->x:F

    float-to-int v0, v0

    iget v1, v9, Landroid/graphics/PointF;->y:F

    float-to-int v1, v1

    invoke-static {p1, v0, v1}, Lv/c;->e(Landroid/view/View;II)Z

    move-result v0

    if-nez v0, :cond_0

    return-void

    :cond_0
    invoke-static {v8}, Ldev/dragshare/catcher/Bridge;->shouldHandleAsViewGroup(Landroid/view/View;)Z

    move-result v0

    const-string v11, "Interceptor_Catcher"

    if-eqz v0, :cond_4

    move-object v0, v8

    check-cast v0, Landroid/view/ViewGroup;

    invoke-virtual {v0}, Landroid/view/ViewGroup;->getChildCount()I

    move-result v1

    iget-object v2, v7, Lcom/miui/contentcatcher/Interceptor;->o:Ljava/util/LinkedList;

    invoke-static {v2}, Lv/c;->a(Ljava/util/LinkedList;)Ljava/util/HashSet;

    move-result-object v2

    invoke-virtual {v2, p1}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_1

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->q()V

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "findTouchView, 1 view="

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v11, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    return-void

    :cond_1
    if-lez v1, :cond_7

    invoke-virtual/range {p4 .. p4}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v2

    if-eqz v2, :cond_7

    iget-boolean v2, v7, Lcom/miui/contentcatcher/Interceptor;->g:Z

    if-nez v2, :cond_7

    const/4 v12, 0x1

    sub-int/2addr v1, v12

    move v13, v1

    :goto_0
    if-ltz v13, :cond_2

    invoke-virtual/range {p4 .. p4}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v1

    if-eqz v1, :cond_2

    iget-object v1, v7, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    if-nez v1, :cond_2

    invoke-virtual {v0, v13}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v2

    move-object v1, p0

    move-object/from16 v3, p2

    move-object/from16 v4, p3

    move-object/from16 v5, p4

    move-object/from16 v6, p5

    invoke-virtual/range {v1 .. v6}, Lcom/miui/contentcatcher/Interceptor;->d(Landroid/view/View;Landroid/graphics/PointF;Landroid/graphics/PointF;Ljava/util/ArrayList;Ljava/util/HashMap;)V

    add-int/lit8 v13, v13, -0x1

    goto :goto_0

    :cond_2
    invoke-virtual/range {p4 .. p4}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v0

    if-eqz v0, :cond_7

    invoke-virtual {p1}, Landroid/view/View;->getBackground()Landroid/graphics/drawable/Drawable;

    move-result-object v0

    if-eqz v0, :cond_7

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "findTouchView, 2 view="

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v1, ",  view.getBackground()="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Landroid/view/View;->getBackground()Landroid/graphics/drawable/Drawable;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v11, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {p1}, Landroid/view/View;->getBackground()Landroid/graphics/drawable/Drawable;

    move-result-object v0

    instance-of v0, v0, Landroid/graphics/drawable/ColorDrawable;

    if-eqz v0, :cond_3

    invoke-virtual {p1}, Landroid/view/View;->getBackground()Landroid/graphics/drawable/Drawable;

    move-result-object v0

    check-cast v0, Landroid/graphics/drawable/ColorDrawable;

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "findTouchView, 2 colorDrawable(alpha:color)=("

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0}, Landroid/graphics/drawable/ColorDrawable;->getAlpha()I

    move-result v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v2, ":"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Landroid/graphics/drawable/ColorDrawable;->getColor()I

    move-result v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v2, ")"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v11, v1}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0}, Landroid/graphics/drawable/ColorDrawable;->getAlpha()I

    move-result v0

    if-nez v0, :cond_3

    return-void

    :cond_3
    iput-boolean v12, v7, Lcom/miui/contentcatcher/Interceptor;->g:Z

    goto/16 :goto_2

    :cond_4
    iget-object v0, v7, Lcom/miui/contentcatcher/Interceptor;->r:Lmiui/contentcatcher/IViewContentFetcher;

    if-eqz v0, :cond_5

    :try_start_0
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v1

    const-string v2, "fetchViewContent"

    const-class v3, Landroid/view/View;

    sget-object v4, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    sget-object v5, Ljava/lang/Boolean;->TYPE:Ljava/lang/Class;

    filled-new-array {v3, v4, v5}, [Ljava/lang/Class;

    move-result-object v3

    invoke-virtual {v1, v2, v3}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v1

    iget v2, v7, Lcom/miui/contentcatcher/Interceptor;->q:I

    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    sget-object v3, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;

    filled-new-array {p1, v2, v3}, [Ljava/lang/Object;

    move-result-object v2

    invoke-virtual {v1, v0, v2}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/os/Bundle;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_1

    :catch_0
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_5
    const/4 v0, 0x0

    :goto_1
    if-eqz v0, :cond_7

    const-string v1, "isPureColor"

    invoke-virtual {v0, v1}, Landroid/os/Bundle;->getBoolean(Ljava/lang/String;)Z

    move-result v1

    const-string v2, "touch_points"

    if-eqz v1, :cond_6

    const-string v1, "screenshot due pure or white-black background"

    invoke-static {v11, v1}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v1, Landroid/os/Bundle;

    invoke-direct {v1}, Landroid/os/Bundle;-><init>()V

    const-string v3, "catchType"

    const/4 v4, 0x2

    invoke-virtual {v1, v3, v4}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    filled-new-array/range {p2 .. p3}, [Landroid/graphics/PointF;

    move-result-object v3

    invoke-virtual {v1, v2, v3}, Landroid/os/Bundle;->putParcelableArray(Ljava/lang/String;[Landroid/os/Parcelable;)V

    iget-object v2, v7, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    invoke-virtual {v2, v1}, Ld/c;->f(Landroid/os/Bundle;)V

    iget-object v1, v7, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;

    const/4 v3, 0x4

    move-object/from16 v4, p5

    invoke-virtual {v2, v3, v1, v4}, Ld/c;->b(ILmiui/contentcatcher/sdk/Token;Ljava/util/HashMap;)V

    new-instance v1, Lcom/miui/contentcatcher/Interceptor$d;

    invoke-direct {v1, p1, v0}, Lcom/miui/contentcatcher/Interceptor$d;-><init>(Landroid/view/View;Landroid/os/Bundle;)V

    invoke-virtual {v10, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    return-void

    :cond_6
    const-string v1, "view_rect"

    invoke-static {p1}, Lv/c;->c(Landroid/view/View;)Landroid/graphics/Rect;

    move-result-object v3

    invoke-virtual {v0, v1, v3}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    filled-new-array/range {p2 .. p3}, [Landroid/graphics/PointF;

    move-result-object v1

    invoke-virtual {v0, v2, v1}, Landroid/os/Bundle;->putParcelableArray(Ljava/lang/String;[Landroid/os/Parcelable;)V

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "findTouchView, 3 view="

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v11, v1}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v1, Lcom/miui/contentcatcher/Interceptor$d;

    invoke-direct {v1, p1, v0}, Lcom/miui/contentcatcher/Interceptor$d;-><init>(Landroid/view/View;Landroid/os/Bundle;)V

    invoke-virtual {v10, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_7
    :goto_2
    return-void
.end method

.method public final dispatchKeyEvent(Landroid/view/KeyEvent;Landroid/view/View;Landroid/app/Activity;)Z
    .locals 1

    const-string p3, "Interceptor_Catcher"

    const/4 v0, 0x0

    :try_start_0
    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->d:Lu/a;

    if-eqz p0, :cond_0

    invoke-virtual {p0, p1, p2}, Lu/a;->b(Landroid/view/KeyEvent;Landroid/view/View;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    return v0

    :catch_0
    move-exception p0

    const-string p1, "dispatchKeyEvent-Error: "

    invoke-static {p1, p0, p3}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_0

    :catch_1
    move-exception p0

    const-string p1, "dispatchKeyEvent-Exception: "

    invoke-static {p1, p0, p3}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_0
    :goto_0
    return v0
.end method

.method public final dispatchTouchEvent(Landroid/view/MotionEvent;Landroid/view/View;Landroid/app/Activity;)Z
    .locals 4


    invoke-static {p1, p2, p3}, Ldev/dragshare/catcher/Bridge;->ping(Landroid/view/MotionEvent;Landroid/view/View;Landroid/app/Activity;)V
    const/4 p3, 0x0

    const-string v0, "Interceptor_Catcher"

    if-nez p1, :cond_0

    const-string p0, "dispatchTouchEvent-event is null"

    invoke-static {v0, p0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    return p3

    :cond_0
    invoke-static {p1}, Landroid/view/MotionEvent;->obtain(Landroid/view/MotionEvent;)Landroid/view/MotionEvent;

    move-result-object v1

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "dispatchTouchEvent: create MotionEvent copy: "

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v2}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v1}, Landroid/view/MotionEvent;->getPointerCount()I

    move-result v2

    if-lez v2, :cond_5

    invoke-virtual {v1}, Landroid/view/MotionEvent;->getHistorySize()I

    move-result v2

    if-gez v2, :cond_1

    goto :goto_4

    :cond_1
    :try_start_0
    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->d:Lu/a;

    if-eqz v2, :cond_2

    invoke-virtual {v2, v1, p2}, Lu/a;->c(Landroid/view/MotionEvent;Landroid/view/View;)V

    invoke-virtual {v1}, Landroid/view/MotionEvent;->getAction()I

    move-result p2

    invoke-virtual {p1}, Landroid/view/MotionEvent;->getAction()I

    move-result v2

    if-eq p2, v2, :cond_2

    invoke-virtual {v1}, Landroid/view/MotionEvent;->getAction()I

    move-result p2

    invoke-virtual {p1, p2}, Landroid/view/MotionEvent;->setAction(I)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_2

    :catch_0
    move-exception p1

    goto :goto_0

    :catch_1
    move-exception p1

    goto :goto_1

    :goto_0
    const-string p2, "dispatchTouchEvent-Error: "

    invoke-static {p2, p1, v0}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_2

    :goto_1
    const-string p2, "dispatchTouchEvent-Exception: "

    invoke-static {p2, p1, v0}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_2
    :goto_2
    iget-boolean p1, p0, Lcom/miui/contentcatcher/Interceptor;->j:Z

    if-eqz p1, :cond_4

    invoke-virtual {v1}, Landroid/view/MotionEvent;->getAction()I

    move-result p1

    if-eqz p1, :cond_3

    invoke-virtual {v1}, Landroid/view/MotionEvent;->getAction()I

    move-result p1

    const/4 p2, 0x1

    if-ne p1, p2, :cond_4

    :cond_3
    invoke-static {}, Landroid/os/Message;->obtain()Landroid/os/Message;

    move-result-object p1

    const/16 p2, 0x67

    iput p2, p1, Landroid/os/Message;->what:I

    iput-object v1, p1, Landroid/os/Message;->obj:Ljava/lang/Object;

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->y:Landroid/os/Handler;

    invoke-virtual {p0, p1}, Landroid/os/Handler;->sendMessage(Landroid/os/Message;)Z

    goto :goto_3

    :cond_4
    invoke-virtual {v1}, Landroid/view/MotionEvent;->recycle()V

    :goto_3
    return p3

    :cond_5
    :goto_4
    new-instance p0, Ljava/lang/StringBuilder;

    const-string p1, "dispatchTouchEvent-malformed event, drop it: pointerCount="

    invoke-direct {p0, p1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1}, Landroid/view/MotionEvent;->getPointerCount()I

    move-result p1

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string p1, ", historySize="

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Landroid/view/MotionEvent;->getHistorySize()I

    move-result p1

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v0, p0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v1}, Landroid/view/MotionEvent;->recycle()V

    return p3
.end method

.method public final e(Landroid/view/View;Ljava/util/ArrayList;I)V
    .locals 4

    if-nez p1, :cond_0

    return-void

    :cond_0
    invoke-static {p1}, Ldev/dragshare/catcher/Bridge;->shouldHandleAsViewGroup(Landroid/view/View;)Z

    move-result v0

    if-nez v0, :cond_1

    invoke-virtual {p0, p1, p2, p3}, Lcom/miui/contentcatcher/Interceptor;->a(Landroid/view/View;Ljava/util/ArrayList;I)V

    return-void

    :cond_1
    sget-object v0, Lv/c;->a:Ljava/util/HashSet;

    new-instance v0, Ljava/util/HashSet;

    invoke-direct {v0}, Ljava/util/HashSet;-><init>()V

    const-string v1, "com.ss.android.lark.imagewidget.largeimage.LargeImageView"

    invoke-virtual {v0, v1}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    const-string v1, "com.miui.newhome.view.GestureImage"

    invoke-virtual {v0, v1}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_2

    invoke-virtual {p0, p1, p2, p3}, Lcom/miui/contentcatcher/Interceptor;->a(Landroid/view/View;Ljava/util/ArrayList;I)V

    return-void

    :cond_2
    move-object v0, p1

    check-cast v0, Landroid/view/ViewGroup;

    invoke-virtual {v0}, Landroid/view/ViewGroup;->getChildCount()I

    move-result v1

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->o:Ljava/util/LinkedList;

    invoke-static {v2}, Lv/c;->a(Ljava/util/LinkedList;)Ljava/util/HashSet;

    move-result-object v2

    invoke-virtual {v2, p1}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z

    move-result v2

    const-string v3, "Interceptor_Catcher"

    if-eqz v2, :cond_3

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->q()V

    new-instance p0, Ljava/lang/StringBuilder;

    const-string p2, "findView, isMyWebView ="

    invoke-direct {p0, p2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v3, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_0

    :cond_3
    instance-of v2, p1, Landroid/webkit/WebView;

    if-eqz v2, :cond_4

    iput-object p1, p0, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    new-instance p0, Ljava/lang/StringBuilder;

    const-string p2, "findView, isWebView ="

    invoke-direct {p0, p2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v3, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    :goto_0
    return-void

    :cond_4
    add-int/lit8 v1, v1, -0x1

    :goto_1
    if-ltz v1, :cond_5

    invoke-virtual {v0, v1}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object p1

    invoke-virtual {p0, p1, p2, p3}, Lcom/miui/contentcatcher/Interceptor;->e(Landroid/view/View;Ljava/util/ArrayList;I)V

    add-int/lit8 v1, v1, -0x1

    goto :goto_1

    :cond_5
    return-void
.end method

.method public final f(Ljava/util/ArrayList;I)Z
    .locals 9
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/ArrayList<",
            "Lcom/miui/contentcatcher/Interceptor$d;",
            ">;I)Z"
        }
    .end annotation

    const/4 v0, 0x1

    const/4 v1, 0x0

    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    const/4 v3, 0x0

    if-nez v2, :cond_0

    return v3

    :cond_0
    invoke-virtual {v2}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v4

    invoke-virtual {v4}, Landroid/view/Window;->getDecorView()Landroid/view/View;

    move-result-object v4

    invoke-virtual {p0, v4, v0}, Lcom/miui/contentcatcher/Interceptor;->j(Landroid/view/View;Z)Z

    move-result v5

    const-string v6, "Interceptor_Catcher"

    if-eqz v5, :cond_1

    new-instance p0, Ljava/lang/StringBuilder;

    const-string p1, "isNotSupportFetchView ="

    invoke-direct {p0, p1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v6, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    return v3

    :cond_1
    sget-object v5, Lv/c;->a:Ljava/util/HashSet;

    new-instance v5, Landroid/util/Size;

    const/16 v7, 0x438

    const/16 v8, 0x7a8

    invoke-direct {v5, v7, v8}, Landroid/util/Size;-><init>(II)V

    :try_start_0
    const-string v5, "window"

    invoke-virtual {v2, v5}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Landroid/view/WindowManager;

    if-eqz v2, :cond_2

    new-instance v5, Landroid/util/DisplayMetrics;

    invoke-direct {v5}, Landroid/util/DisplayMetrics;-><init>()V

    invoke-interface {v2}, Landroid/view/WindowManager;->getDefaultDisplay()Landroid/view/Display;

    move-result-object v2

    invoke-virtual {v2, v5}, Landroid/view/Display;->getMetrics(Landroid/util/DisplayMetrics;)V

    new-instance v2, Landroid/util/Size;

    iget v7, v5, Landroid/util/DisplayMetrics;->widthPixels:I

    iget v5, v5, Landroid/util/DisplayMetrics;->heightPixels:I

    invoke-direct {v2, v7, v5}, Landroid/util/Size;-><init>(II)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    goto :goto_0

    :catchall_0
    move-exception v2

    new-instance v5, Ljava/lang/StringBuilder;

    const-string v7, "getScreenSize(): "

    invoke-direct {v5, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v5, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    const-string v5, "ContentCatcher.viewUtils"

    invoke-static {v5, v2}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    :cond_2
    :goto_0
    invoke-virtual {p0, v4, p1, p2}, Lcom/miui/contentcatcher/Interceptor;->e(Landroid/view/View;Ljava/util/ArrayList;I)V

    iget-object p1, p0, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    if-eqz p1, :cond_3

    move p1, v0

    goto :goto_1

    :cond_3
    move p1, v3

    :goto_1
    new-instance v2, Ljava/lang/StringBuilder;

    const-string v4, "findViewImpl hasWebView: "

    invoke-direct {v2, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    const-string v4, ", type:"

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v6, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    if-eqz p1, :cond_8

    if-eq p2, v0, :cond_7

    const/4 v2, 0x2

    if-eq p2, v2, :cond_9

    const/4 v2, 0x3

    if-eq p2, v2, :cond_4

    goto :goto_2

    :cond_4
    iget-object p2, p0, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    if-nez p2, :cond_5

    const-string p2, "fetchWebViewAll mTouchWebView = null"

    invoke-static {v6, p2}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_2

    :cond_5
    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->c()V

    const-string p2, "prepare fetchWebViewUrl"

    invoke-static {v6, p2}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p2, p0, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    if-nez p2, :cond_6

    const-string p2, "prepare fetchWebViewUrl mTouchWebView = null"

    invoke-static {v6, p2}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_2

    :cond_6
    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->u:Ly/g;

    if-eqz v2, :cond_8

    invoke-static {p2}, Ld/a;->c(Landroid/view/View;)Ls/h;

    move-result-object p2

    if-eqz p2, :cond_8

    new-instance v2, Lm/b;

    invoke-direct {v2, p0}, Lm/b;-><init>(Lcom/miui/contentcatcher/Interceptor;)V

    const-string v3, "WebViewWrapperBase"

    const-string v4, "fetchWebViewUrl prepare exec js"

    invoke-static {v3, v4}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v3, Ls/g;

    invoke-direct {v3, v2, v0}, Ls/g;-><init>(Landroid/webkit/ValueCallback;I)V

    const-string v0, "window.location.href"

    invoke-virtual {p2, v0, v3}, Ls/h;->b(Ljava/lang/String;Landroid/webkit/ValueCallback;)V

    goto :goto_2

    :cond_7
    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->c()V

    :cond_8
    :goto_2
    move v3, p1

    :cond_9
    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    return v3
.end method

.method public final g()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    if-nez p0, :cond_0

    const-string p0, ""

    return-object p0

    :cond_0
    invoke-virtual {p0}, Landroid/app/Activity;->getComponentName()Landroid/content/ComponentName;

    move-result-object p0

    invoke-virtual {p0}, Landroid/content/ComponentName;->getClassName()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method

.method public final h()Landroid/os/Handler;
    .locals 3

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->z:Landroid/os/Handler;

    if-nez v0, :cond_1

    const-class v0, Lr/b;

    monitor-enter v0

    :try_start_0
    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->z:Landroid/os/Handler;

    if-nez v1, :cond_0

    new-instance v1, Landroid/os/Handler;

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    move-result-object v2

    invoke-direct {v1, v2}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->z:Landroid/os/Handler;

    goto :goto_0

    :catchall_0
    move-exception p0

    goto :goto_1

    :cond_0
    :goto_0
    monitor-exit v0

    goto :goto_2

    :goto_1
    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw p0

    :cond_1
    :goto_2
    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->z:Landroid/os/Handler;

    return-object p0
.end method

.method public final i()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    if-nez p0, :cond_0

    const-string p0, ""

    return-object p0

    :cond_0
    invoke-virtual {p0}, Landroid/app/Activity;->getComponentName()Landroid/content/ComponentName;

    move-result-object p0

    invoke-virtual {p0}, Landroid/content/ComponentName;->getPackageName()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method

.method public final j(Landroid/view/View;Z)Z
    .locals 6

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    const/4 v1, 0x0

    const/4 v2, 0x1

    const-string v3, "Interceptor_Catcher"

    if-nez p2, :cond_2

    if-nez v0, :cond_0

    :goto_0
    move p2, v1

    goto :goto_1

    :cond_0
    :try_start_0
    invoke-virtual {v0}, Landroid/app/Activity;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object p2

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->i()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {p2, v4, v2}, Landroid/content/pm/PackageManager;->getPackageUid(Ljava/lang/String;I)I

    move-result p2
    :try_end_0
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_1

    :catch_0
    move-exception p2

    invoke-virtual {p2}, Ljava/lang/Throwable;->printStackTrace()V

    goto :goto_0

    :goto_1
    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->i()Ljava/lang/String;

    move-result-object v4

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->t:Landroid/app/AppOpsManager;

    const-string v5, "android:camera"

    invoke-virtual {p0, v5, p2, v4}, Landroid/app/AppOpsManager;->isOpActive(Ljava/lang/String;ILjava/lang/String;)Z

    move-result v5

    if-nez v5, :cond_1

    const-string v5, "android:record_audio"

    invoke-virtual {p0, v5, p2, v4}, Landroid/app/AppOpsManager;->isOpActive(Ljava/lang/String;ILjava/lang/String;)Z

    move-result p0

    if-eqz p0, :cond_2

    :cond_1
    const-string p0, "audio and camera not trigger contentCatcher"

    invoke-static {v3, p0}, Landroid/util/Slog;->i(Ljava/lang/String;Ljava/lang/String;)I

    return v2

    :cond_2
    if-eqz v0, :cond_3

    :try_start_1
    invoke-virtual {v0}, Landroid/app/Activity;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object p0

    invoke-virtual {v0}, Landroid/app/Activity;->getComponentName()Landroid/content/ComponentName;

    move-result-object p2

    const/16 v0, 0x80

    invoke-virtual {p0, p2, v0}, Landroid/content/pm/PackageManager;->getActivityInfo(Landroid/content/ComponentName;I)Landroid/content/pm/ActivityInfo;

    move-result-object p0

    iget-object p0, p0, Landroid/content/pm/ActivityInfo;->metaData:Landroid/os/Bundle;

    if-eqz p0, :cond_3

    const-string p2, "catcher_not_fetch"

    invoke-virtual {p0, p2}, Landroid/os/Bundle;->getBoolean(Ljava/lang/String;)Z

    move-result p0

    if-eqz p0, :cond_3

    const-string p0, "current activity not support contentCatcher"

    invoke-static {v3, p0}, Landroid/util/Slog;->i(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_1
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_1 .. :try_end_1} :catch_1

    const-string p0, "is catcher not fetcher"

    invoke-static {v3, p0}, Landroid/util/Slog;->i(Ljava/lang/String;Ljava/lang/String;)I

    return v2

    :catch_1
    move-exception p0

    invoke-virtual {p0}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_3
    if-nez p1, :cond_4

    const-string p0, "performPickModeByAccessibility rootView is null"

    invoke-static {v3, p0}, Landroid/util/Slog;->i(Ljava/lang/String;Ljava/lang/String;)I

    return v2

    :cond_4
    const-string p0, "isSecureUI lp is not WindowManager.LayoutParams: "

    :try_start_2
    invoke-virtual {p1}, Landroid/view/View;->getRootView()Landroid/view/View;

    move-result-object p1

    invoke-virtual {p1}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object p1

    instance-of p2, p1, Landroid/view/WindowManager$LayoutParams;

    if-eqz p2, :cond_5

    check-cast p1, Landroid/view/WindowManager$LayoutParams;

    iget p0, p1, Landroid/view/WindowManager$LayoutParams;->flags:I
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_2

    and-int/lit16 p0, p0, 0x2000

    if-eqz p0, :cond_6

    const-string p0, "is secure ui cannot fetch"

    invoke-static {v3, p0}, Landroid/util/Slog;->i(Ljava/lang/String;Ljava/lang/String;)I

    return v2

    :catch_2
    move-exception p0

    goto :goto_2

    :cond_5
    :try_start_3
    new-instance p2, Ljava/lang/StringBuilder;

    invoke-direct {p2, p0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v3, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_2

    goto :goto_3

    :goto_2
    const-string p1, "isSecureUI error: "

    invoke-static {p1, p0, v3}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_6
    :goto_3
    return v1
.end method

.method public final k(ILjava/util/LinkedHashMap;)V
    .locals 4

    const-string v0, "WebViewCatcher"

    const/4 v1, 0x1

    iput-boolean v1, p0, Lcom/miui/contentcatcher/Interceptor;->i:Z

    if-nez p2, :cond_0

    new-instance p2, Ljava/util/HashMap;

    invoke-direct {p2}, Ljava/util/HashMap;-><init>()V

    :cond_0
    if-ne p1, v1, :cond_1

    const-string v1, "activityidentity"

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->h:Ljava/lang/String;

    invoke-interface {p2, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :cond_1
    const-string v1, "ismultiscreen"

    invoke-interface {p2, v1}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    if-nez v1, :cond_3

    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    sget-object v2, Lv/c;->a:Ljava/util/HashSet;

    invoke-virtual {v1}, Landroid/app/Activity;->isInMultiWindowMode()Z

    move-result v1

    const-string v2, "ismultiscreen"

    if-eqz v1, :cond_2

    const-string v1, "1"

    goto :goto_0

    :cond_2
    const-string v1, "0"

    :goto_0
    invoke-interface {p2, v2, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    :cond_3
    sget-boolean v1, Lp/b;->a:Z

    if-eqz v1, :cond_4

    const-string v1, "Interceptor_Catcher"

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "onContentCatched: "

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p2}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    :cond_4
    const-string v1, "Interceptor_Catcher"

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "onContentCatched mHasContent: "

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-boolean v3, p0, Lcom/miui/contentcatcher/Interceptor;->i:Z

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    monitor-enter p0

    :try_start_0
    new-instance v1, Lmiui/contentcatcher/sdk/Content;

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;

    new-instance v3, Ljava/util/HashMap;

    invoke-direct {v3, p2}, Ljava/util/HashMap;-><init>(Ljava/util/Map;)V

    invoke-direct {v1, p1, v0, v2, v3}, Lmiui/contentcatcher/sdk/Content;-><init>(ILjava/lang/String;Lmiui/contentcatcher/sdk/Token;Ljava/util/Map;)V

    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->s:Lmiui/contentcatcher/sdk/Content;

    monitor-exit p0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    iget-object p1, p0, Lcom/miui/contentcatcher/Interceptor;->y:Landroid/os/Handler;

    new-instance p2, Lm/a;

    const/4 v0, 0x0

    invoke-direct {p2, p0, v0}, Lm/a;-><init>(Ljava/lang/Object;I)V

    const-wide/16 v0, 0x3e8

    invoke-virtual {p1, p2, v0, v1}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    return-void

    :catchall_0
    move-exception p1

    :try_start_1
    monitor-exit p0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    throw p1
.end method

.method public final l(Landroid/graphics/PointF;Landroid/graphics/PointF;Landroid/view/View;)Z
    .locals 17


    invoke-static/range {p1 .. p3}, Ldev/dragshare/catcher/Bridge;->recordTouch(Landroid/graphics/PointF;Landroid/graphics/PointF;Landroid/view/View;)V
    move-object/from16 v7, p0

    move-object/from16 v8, p1

    move-object/from16 v2, p3

    const/4 v9, 0x1

    iget-object v0, v7, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    const/4 v10, 0x0

    if-eqz v0, :cond_0

    invoke-virtual {v0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v0

    invoke-virtual {v0}, Landroid/view/Window;->getDecorView()Landroid/view/View;

    move-result-object v0

    if-ne v2, v0, :cond_0

    move v0, v9

    goto :goto_0

    :cond_0
    move v0, v10

    :goto_0
    if-eqz v0, :cond_16

    iget-object v0, v7, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    if-eqz v0, :cond_16

    if-eqz v2, :cond_16

    new-instance v11, Ljava/util/HashMap;

    invoke-direct {v11}, Ljava/util/HashMap;-><init>()V

    iget-object v0, v7, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    sget-object v1, Lv/c;->a:Ljava/util/HashSet;

    invoke-virtual {v0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v1

    invoke-virtual {v1}, Landroid/view/Window;->getDecorView()Landroid/view/View;

    move-result-object v1

    invoke-virtual {v1}, Landroid/view/View;->getSystemUiVisibility()I

    move-result v1

    and-int/lit16 v3, v1, 0x1000

    if-eqz v3, :cond_1

    move v3, v9

    goto :goto_1

    :cond_1
    move v3, v10

    :goto_1
    invoke-virtual {v0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v0

    invoke-virtual {v0}, Landroid/view/Window;->getAttributes()Landroid/view/WindowManager$LayoutParams;

    move-result-object v0

    iget v0, v0, Landroid/view/WindowManager$LayoutParams;->flags:I

    and-int/lit16 v0, v0, 0x400

    if-eqz v0, :cond_2

    move v0, v9

    goto :goto_2

    :cond_2
    move v0, v10

    :goto_2
    const/4 v12, 0x4

    and-int/2addr v1, v12

    if-eqz v1, :cond_3

    move v1, v9

    goto :goto_3

    :cond_3
    move v1, v10

    :goto_3
    if-nez v0, :cond_5

    if-eqz v1, :cond_4

    if-eqz v3, :cond_4

    goto :goto_4

    :cond_4
    move v1, v10

    goto :goto_5

    :cond_5
    :goto_4
    move v1, v9

    :goto_5
    iget-object v0, v7, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    :try_start_0
    invoke-virtual {v0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v0

    invoke-virtual {v0}, Landroid/view/Window;->getDecorView()Landroid/view/View;

    move-result-object v0

    invoke-static {v0, v10}, Lv/c;->b(Landroid/view/View;I)Landroid/view/ViewGroup;

    move-result-object v0

    if-eqz v0, :cond_7

    invoke-virtual {v0}, Landroid/view/ViewGroup;->getVisibility()I

    move-result v3

    if-nez v3, :cond_7

    invoke-virtual {v0}, Landroid/view/ViewGroup;->getVisibility()I

    move-result v3
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    if-eqz v3, :cond_6

    goto :goto_6

    :cond_6
    :try_start_1
    invoke-virtual {v0, v9}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/view/ViewGroup;

    if-eqz v0, :cond_7

    invoke-virtual {v0}, Landroid/view/ViewGroup;->getChildCount()I

    move-result v3

    const/4 v4, 0x2

    if-lt v3, v4, :cond_7

    invoke-virtual {v0, v9}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v0

    instance-of v3, v0, Landroid/widget/Button;

    if-eqz v3, :cond_7

    invoke-virtual {v0}, Landroid/view/View;->getVisibility()I

    move-result v0
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1

    if-nez v0, :cond_7

    move v0, v9

    goto :goto_7

    :catch_0
    move-exception v0

    const-string v3, "e: "

    const-string v4, "ContentCatcher.viewUtils"

    invoke-static {v3, v0, v4}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :catch_1
    :cond_7
    :goto_6
    move v0, v10

    :goto_7
    iget-object v3, v7, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    invoke-virtual {v3}, Landroid/app/Activity;->isInMultiWindowMode()Z

    move-result v3

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    iget v5, v8, Landroid/graphics/PointF;->x:F

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;

    const-string v5, "-"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v5, v8, Landroid/graphics/PointF;->y:F

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    const-string v5, "isfullscreen"

    if-eqz v1, :cond_8

    const-string v1, "1"

    goto :goto_8

    :cond_8
    const-string v1, "0"

    :goto_8
    invoke-virtual {v11, v5, v1}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    const-string v1, "actionbarvisible"

    if-eqz v0, :cond_9

    const-string v0, "1"

    goto :goto_9

    :cond_9
    const-string v0, "0"

    :goto_9
    invoke-virtual {v11, v1, v0}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    const-string v0, "coordinate"

    invoke-virtual {v11, v0, v4}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    const-string v0, "ismultiscreen"

    if-eqz v3, :cond_a

    const-string v1, "1"

    goto :goto_a

    :cond_a
    const-string v1, "0"

    :goto_a
    invoke-virtual {v11, v0, v1}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    invoke-virtual {v7, v2, v10}, Lcom/miui/contentcatcher/Interceptor;->j(Landroid/view/View;Z)Z

    move-result v0

    if-eqz v0, :cond_c

    :cond_b
    :goto_b
    move v9, v10

    goto/16 :goto_f

    :cond_c
    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v13

    iput-boolean v10, v7, Lcom/miui/contentcatcher/Interceptor;->g:Z

    const/4 v15, 0x0

    iput-object v15, v7, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    sget-boolean v16, Ls/e;->m:Z

    move-object/from16 v1, p0

    move-object/from16 v2, p3

    move-object/from16 v3, p1

    move-object/from16 v4, p2

    move-object v5, v0

    move-object v6, v11

    invoke-virtual/range {v1 .. v6}, Lcom/miui/contentcatcher/Interceptor;->d(Landroid/view/View;Landroid/graphics/PointF;Landroid/graphics/PointF;Ljava/util/ArrayList;Ljava/util/HashMap;)V

    const-string v1, "Interceptor_Catcher"

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "performPickModeByAccessibility, mFindTouchTarget="

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-boolean v3, v7, Lcom/miui/contentcatcher/Interceptor;->g:Z

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    const-string v3, ", mTouchWebView="

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v3, v7, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v3, ", results.size="

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/util/ArrayList;->size()I

    move-result v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    const-string v1, "Interceptor_Catcher"

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "performPickModeByAccessibility mHasContent: "

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-boolean v3, v7, Lcom/miui/contentcatcher/Interceptor;->i:Z

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    const-string v1, "Interceptor_Catcher"

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "find view took "

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v3

    sub-long/2addr v3, v13

    invoke-virtual {v2, v3, v4}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    const-string v3, " ms"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->i(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v1

    if-eqz v1, :cond_e

    if-eqz v16, :cond_e

    iget-object v1, v7, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    if-eqz v1, :cond_e

    iget-boolean v2, v7, Lcom/miui/contentcatcher/Interceptor;->i:Z

    if-nez v2, :cond_e

    iget-object v0, v7, Lcom/miui/contentcatcher/Interceptor;->r:Lmiui/contentcatcher/IViewContentFetcher;

    iget v2, v7, Lcom/miui/contentcatcher/Interceptor;->q:I

    invoke-interface {v0, v1, v2}, Lmiui/contentcatcher/IViewContentFetcher;->fetchWebViewBitmap(Landroid/view/View;I)Landroid/os/Bundle;

    move-result-object v0

    if-nez v0, :cond_d

    goto/16 :goto_b

    :cond_d
    const-string v1, "view_rect"

    iget-object v2, v7, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    invoke-static {v2}, Lv/c;->c(Landroid/view/View;)Landroid/graphics/Rect;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    const-string v1, "touch_points"

    filled-new-array/range {p1 .. p2}, [Landroid/graphics/PointF;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putParcelableArray(Ljava/lang/String;[Landroid/os/Parcelable;)V

    iput-object v15, v7, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    new-instance v1, Lcom/miui/contentcatcher/Interceptor$e;

    invoke-direct {v1, v7}, Lcom/miui/contentcatcher/Interceptor$e;-><init>(Lcom/miui/contentcatcher/Interceptor;)V

    iput-object v11, v1, Lcom/miui/contentcatcher/Interceptor$e;->a:Ljava/util/HashMap;

    iput-object v0, v1, Lcom/miui/contentcatcher/Interceptor$e;->b:Landroid/os/Bundle;

    iget-object v0, v7, Lcom/miui/contentcatcher/Interceptor;->y:Landroid/os/Handler;

    const-wide/16 v2, 0x1f4

    invoke-virtual {v0, v1, v2, v3}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    goto/16 :goto_f

    :cond_e
    invoke-virtual {v0}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v1

    if-eqz v1, :cond_13

    if-eqz v16, :cond_13

    iget-object v1, v7, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    if-eqz v1, :cond_13

    iget-boolean v1, v7, Lcom/miui/contentcatcher/Interceptor;->i:Z

    if-eqz v1, :cond_13

    monitor-enter p0

    :try_start_2
    iget-object v0, v7, Lcom/miui/contentcatcher/Interceptor;->s:Lmiui/contentcatcher/sdk/Content;

    if-eqz v0, :cond_12

    iget-object v0, v0, Lmiui/contentcatcher/sdk/Content;->mResultMap:Ljava/util/Map;

    invoke-interface {v0}, Ljava/util/Map;->entrySet()Ljava/util/Set;

    move-result-object v0

    invoke-interface {v0}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :cond_f
    :goto_c
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_11

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/util/Map$Entry;

    invoke-interface {v1}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/String;

    const-string v3, "pick_mode"

    invoke-virtual {v2, v3}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v2

    if-eqz v2, :cond_f

    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v2

    if-eqz v2, :cond_f

    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v2

    instance-of v2, v2, Ljava/lang/String;

    if-eqz v2, :cond_10

    new-instance v2, Landroid/os/Bundle;

    invoke-direct {v2}, Landroid/os/Bundle;-><init>()V

    const-string v3, "viewContent"

    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/String;

    invoke-virtual {v2, v3, v4}, Landroid/os/Bundle;->putString(Ljava/lang/String;Ljava/lang/String;)V

    const-string v3, "view_rect"

    iget-object v4, v7, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    invoke-static {v4}, Lv/c;->c(Landroid/view/View;)Landroid/graphics/Rect;

    move-result-object v4

    invoke-virtual {v2, v3, v4}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    const-string v3, "touch_points"

    filled-new-array/range {p1 .. p2}, [Landroid/graphics/PointF;

    move-result-object v4

    invoke-virtual {v2, v3, v4}, Landroid/os/Bundle;->putParcelableArray(Ljava/lang/String;[Landroid/os/Parcelable;)V

    invoke-interface {v1, v2}, Ljava/util/Map$Entry;->setValue(Ljava/lang/Object;)Ljava/lang/Object;

    goto :goto_c

    :catchall_0
    move-exception v0

    goto :goto_d

    :cond_10
    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v2

    instance-of v2, v2, Landroid/os/Bundle;

    if-eqz v2, :cond_f

    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/os/Bundle;

    const-string v2, "view_rect"

    iget-object v3, v7, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    invoke-static {v3}, Lv/c;->c(Landroid/view/View;)Landroid/graphics/Rect;

    move-result-object v3

    invoke-virtual {v1, v2, v3}, Landroid/os/Bundle;->putParcelable(Ljava/lang/String;Landroid/os/Parcelable;)V

    const-string v2, "touch_points"

    filled-new-array/range {p1 .. p2}, [Landroid/graphics/PointF;

    move-result-object v3

    invoke-virtual {v1, v2, v3}, Landroid/os/Bundle;->putParcelableArray(Ljava/lang/String;[Landroid/os/Parcelable;)V

    goto :goto_c

    :cond_11
    iget-object v0, v7, Lcom/miui/contentcatcher/Interceptor;->s:Lmiui/contentcatcher/sdk/Content;

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "dispatchContent, content: "

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    const-string v2, "CatcherHolder"

    invoke-static {v2, v1}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {v0}, Ldev/dragshare/catcher/Bridge;->deliverContent(Ljava/lang/Object;)V

    :cond_12
    monitor-exit p0
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    iput-object v15, v7, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    goto :goto_f

    :goto_d
    :try_start_3
    monitor-exit p0
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    throw v0

    :cond_13
    iput-boolean v10, v7, Lcom/miui/contentcatcher/Interceptor;->i:Z

    iput-object v15, v7, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    invoke-virtual {v0}, Ljava/util/ArrayList;->size()I

    move-result v1

    move v2, v10

    :goto_e
    if-ge v2, v1, :cond_b

    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Lcom/miui/contentcatcher/Interceptor$d;

    iget-object v4, v3, Lcom/miui/contentcatcher/Interceptor$d;->a:Landroid/view/View;

    instance-of v5, v4, Landroid/widget/EditText;

    if-nez v5, :cond_b

    invoke-virtual {v4}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v4

    const-string v5, "EditText"

    invoke-virtual {v4, v5}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v4

    if-eqz v4, :cond_14

    goto/16 :goto_b

    :cond_14
    iget-object v4, v3, Lcom/miui/contentcatcher/Interceptor$d;->b:Landroid/os/Bundle;

    invoke-virtual {v4}, Landroid/os/Bundle;->isEmpty()Z

    move-result v4

    if-nez v4, :cond_15

    iget-object v0, v7, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    iget-object v1, v3, Lcom/miui/contentcatcher/Interceptor$d;->b:Landroid/os/Bundle;

    invoke-virtual {v0, v1}, Ld/c;->f(Landroid/os/Bundle;)V

    const-string v0, "isedittext"

    const-string v1, "0"

    invoke-virtual {v11, v0, v1}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    iget-object v0, v7, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    iget-object v1, v7, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;

    invoke-virtual {v0, v12, v1, v11}, Ld/c;->b(ILmiui/contentcatcher/sdk/Token;Ljava/util/HashMap;)V

    goto :goto_f

    :cond_15
    add-int/2addr v2, v9

    goto :goto_e

    :goto_f
    move v10, v9

    :cond_16
    return v10
.end method

.method public final m()V
    .locals 13

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    if-eqz v0, :cond_4

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v1

    new-instance v3, Ljava/lang/StringBuilder;

    const-string v4, "performCatchContent of activity "

    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v4, " for reason 1"

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    const-string v4, "Interceptor_Catcher"

    invoke-static {v4, v3}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v0

    invoke-virtual {v0}, Landroid/view/Window;->getDecorView()Landroid/view/View;

    move-result-object v0

    iget-object v3, p0, Lcom/miui/contentcatcher/Interceptor;->o:Ljava/util/LinkedList;

    invoke-static {v3}, Lv/c;->a(Ljava/util/LinkedList;)Ljava/util/HashSet;

    move-result-object v3

    iget-object v5, p0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    invoke-virtual {v5}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    new-instance v6, Ljava/lang/StringBuilder;

    const-string v7, "startCatchContent of view "

    invoke-direct {v6, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v6, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v7, ", catch type 1"

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v6

    const-string v7, "CatcherHolder"

    invoke-static {v7, v6}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    if-eqz v0, :cond_3

    iget-object v6, v5, Ld/c;->a:Ljava/io/Serializable;

    check-cast v6, Ljava/util/ArrayList;

    invoke-virtual {v6}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v8

    if-eqz v8, :cond_0

    goto/16 :goto_2

    :cond_0
    invoke-virtual {v5}, Ld/c;->g()V

    invoke-virtual {v6}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v6

    :goto_0
    invoke-interface {v6}, Ljava/util/Iterator;->hasNext()Z

    move-result v8

    if-eqz v8, :cond_2

    invoke-interface {v6}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v8

    check-cast v8, Lr/b;

    new-instance v9, Ljava/lang/StringBuilder;

    const-string v10, "Catcher: "

    invoke-direct {v9, v10}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v8}, Lr/b;->e()Ljava/lang/String;

    move-result-object v10

    invoke-virtual {v9, v10}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v10, "-"

    invoke-virtual {v9, v10}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    new-instance v10, Ljava/lang/StringBuilder;

    invoke-direct {v10}, Ljava/lang/StringBuilder;-><init>()V

    iget-object v8, v8, Lr/b;->b:Ljava/util/ArrayList;

    invoke-virtual {v8}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v8

    :goto_1
    invoke-interface {v8}, Ljava/util/Iterator;->hasNext()Z

    move-result v11

    if-eqz v11, :cond_1

    invoke-interface {v8}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v11

    check-cast v11, Lt/a;

    new-instance v12, Ljava/lang/StringBuilder;

    invoke-direct {v12}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v11}, Lt/a;->toString()Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v12, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v11, "---\n"

    invoke-virtual {v12, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v12}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    goto :goto_1

    :cond_1
    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v8

    invoke-virtual {v9, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v9}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v8

    invoke-static {v7, v8}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_0

    :cond_2
    :try_start_0
    instance-of v6, v0, Landroid/view/ViewGroup;

    if-eqz v6, :cond_3

    check-cast v0, Landroid/view/ViewGroup;

    invoke-virtual {v5, v0, v3}, Ld/c;->h(Landroid/view/View;Ljava/util/HashSet;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_2

    :catch_0
    move-exception v0

    const-string v3, "traverseViewTree Exception: "

    invoke-static {v3, v0, v7}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_3
    :goto_2
    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    const-string v3, "activityidentity"

    iget-object v6, p0, Lcom/miui/contentcatcher/Interceptor;->h:Ljava/lang/String;

    invoke-virtual {v0, v3, v6}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;

    const/4 v3, 0x1

    invoke-virtual {v5, v3, p0, v0}, Ld/c;->b(ILmiui/contentcatcher/sdk/Token;Ljava/util/HashMap;)V

    new-instance p0, Ljava/lang/StringBuilder;

    const-string v0, "performCatchContent took "

    invoke-direct {p0, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v5

    sub-long/2addr v5, v1

    invoke-virtual {p0, v5, v6}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    const-string v0, "ms"

    invoke-virtual {p0, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v4, p0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    :cond_4
    return-void
.end method

.method public final n()V
    .locals 18

    move-object/from16 v0, p0

    iget-object v1, v0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    iget-object v2, v1, Ld/c;->a:Ljava/io/Serializable;

    check-cast v2, Ljava/util/ArrayList;

    invoke-virtual {v2}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v3

    :goto_0
    invoke-interface {v3}, Ljava/util/Iterator;->hasNext()Z

    move-result v4

    const/4 v5, 0x0

    if-eqz v4, :cond_0

    invoke-interface {v3}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lr/b;

    invoke-virtual {v4}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    iput-object v5, v4, Lr/b;->c:Lcom/miui/contentcatcher/Interceptor;

    iget-object v6, v4, Lr/b;->k:Ljava/lang/Object;

    monitor-enter v6

    :try_start_0
    iput-object v5, v4, Lr/b;->d:Lcom/miui/contentcatcher/Interceptor;

    monitor-exit v6

    goto :goto_0

    :catchall_0
    move-exception v0

    monitor-exit v6
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw v0

    :cond_0
    invoke-virtual {v2}, Ljava/util/ArrayList;->clear()V

    invoke-virtual {v1}, Ld/c;->g()V

    iget-object v2, v0, Lcom/miui/contentcatcher/Interceptor;->d:Lu/a;

    if-eqz v2, :cond_1

    iget-object v2, v2, Lu/a;->g:Lu/a$a;

    invoke-virtual {v2, v5}, Landroid/os/Handler;->removeCallbacksAndMessages(Ljava/lang/Object;)V

    iput-object v5, v0, Lcom/miui/contentcatcher/Interceptor;->d:Lu/a;

    :cond_1
    iget-object v3, v0, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;

    invoke-static {v3}, Ldev/dragshare/catcher/Bridge;->getPageConfig(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lmiui/contentcatcher/sdk/data/PageConfig;

    const-string v4, "Interceptor_Catcher"

    iget-object v6, v0, Lcom/miui/contentcatcher/Interceptor;->b:Ljava/lang/String;

    if-eqz v2, :cond_28

    invoke-virtual {v2}, Lmiui/contentcatcher/sdk/data/PageConfig;->getFeatures()Ljava/util/ArrayList;

    move-result-object v7

    invoke-virtual {v7}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v7

    if-eqz v7, :cond_2

    invoke-virtual {v2}, Lmiui/contentcatcher/sdk/data/PageConfig;->getCatchers()Ljava/util/ArrayList;

    move-result-object v7

    invoke-virtual {v7}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v7

    if-eqz v7, :cond_2

    goto/16 :goto_18

    :cond_2
    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "interest in this page: "

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v7, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-static {v4, v7}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v2}, Lmiui/contentcatcher/sdk/data/PageConfig;->getCatchers()Ljava/util/ArrayList;

    move-result-object v7

    const-string v8, "CatcherImpl"

    iget-object v9, v0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    const/4 v11, 0x1

    if-eqz v7, :cond_9

    invoke-interface {v7}, Ljava/util/List;->isEmpty()Z

    move-result v12

    if-eqz v12, :cond_3

    goto/16 :goto_4

    :cond_3
    invoke-interface {v7}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v7

    :goto_1
    invoke-interface {v7}, Ljava/util/Iterator;->hasNext()Z

    move-result v12

    if-eqz v12, :cond_a

    invoke-interface {v7}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v12

    check-cast v12, Lmiui/contentcatcher/sdk/data/CatcherInfo;

    new-instance v13, Ljava/lang/StringBuilder;

    const-string v14, "Get catcher from config "

    invoke-direct {v13, v14}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v14, v12, Lmiui/contentcatcher/sdk/data/CatcherInfo;->name:Ljava/lang/String;

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v13}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v13

    invoke-static {v4, v13}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v13, v12, Lmiui/contentcatcher/sdk/data/CatcherInfo;->name:Ljava/lang/String;

    invoke-static {v13}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v13

    if-nez v13, :cond_8

    iget-object v13, v12, Lmiui/contentcatcher/sdk/data/CatcherInfo;->name:Ljava/lang/String;

    invoke-static {v13, v9, v0}, Lr/a;->a(Ljava/lang/String;Landroid/app/Activity;Lcom/miui/contentcatcher/Interceptor;)Lr/b;

    move-result-object v13

    if-eqz v13, :cond_8

    new-instance v14, Ljava/lang/StringBuilder;

    const-string v15, "parseCatcherInfo catcherInfo \uff1a "

    invoke-direct {v14, v15}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v12}, Lmiui/contentcatcher/sdk/data/CatcherInfo;->toString()Ljava/lang/String;

    move-result-object v15

    invoke-virtual {v14, v15}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v14}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v14

    invoke-static {v8, v14}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v12, v12, Lmiui/contentcatcher/sdk/data/CatcherInfo;->mParams:Ljava/util/Map;

    if-eqz v12, :cond_7

    invoke-virtual {v13, v12}, Lr/b;->i(Ljava/util/Map;)V

    invoke-interface {v12}, Ljava/util/Map;->keySet()Ljava/util/Set;

    move-result-object v14

    invoke-interface {v14}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v14

    :goto_2
    invoke-interface {v14}, Ljava/util/Iterator;->hasNext()Z

    move-result v15

    if-eqz v15, :cond_7

    invoke-interface {v14}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v15

    check-cast v15, Ljava/lang/String;

    if-eqz v15, :cond_6

    new-instance v5, Lt/a;

    invoke-direct {v5, v15, v11}, Lt/a;-><init>(Ljava/lang/String;I)V

    invoke-interface {v12, v15}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v16

    move-object/from16 v11, v16

    check-cast v11, Ljava/lang/String;

    const-string v10, "#action"

    invoke-virtual {v15, v10}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v10

    if-eqz v10, :cond_4

    const-string v10, "all"

    invoke-virtual {v11, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v10

    if-eqz v10, :cond_5

    const/4 v10, 0x2

    iput v10, v5, Lt/a;->d:I

    goto :goto_3

    :cond_4
    const/4 v10, 0x1

    iput v10, v5, Lt/a;->d:I

    iput-object v11, v5, Lt/a;->c:Ljava/lang/String;

    :cond_5
    :goto_3
    iget-object v10, v13, Lr/b;->b:Ljava/util/ArrayList;

    invoke-virtual {v10, v5}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_6
    const/4 v5, 0x0

    const/4 v11, 0x1

    goto :goto_2

    :cond_7
    iget-object v5, v1, Ld/c;->a:Ljava/io/Serializable;

    check-cast v5, Ljava/util/ArrayList;

    invoke-virtual {v5, v13}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_8
    const/4 v5, 0x0

    const/4 v11, 0x1

    goto/16 :goto_1

    :cond_9
    :goto_4
    new-instance v5, Ljava/lang/StringBuilder;

    const-string v7, "Catcher list invalid for "

    invoke-direct {v5, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-static {v4, v5}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    :cond_a
    invoke-virtual {v2}, Lmiui/contentcatcher/sdk/data/PageConfig;->getFeatures()Ljava/util/ArrayList;

    move-result-object v2

    if-eqz v2, :cond_b

    invoke-interface {v2}, Ljava/util/List;->isEmpty()Z

    move-result v7

    if-eqz v7, :cond_c

    :cond_b
    const/4 v2, 0x1

    goto/16 :goto_e

    :cond_c
    invoke-interface {v2}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v2

    :goto_5
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    move-result v7

    if-eqz v7, :cond_1c

    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;

    new-instance v10, Ljava/lang/StringBuilder;

    const-string v11, "Get featureInfo from config "

    invoke-direct {v10, v11}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v11, v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;->name:Ljava/lang/String;

    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v10

    invoke-static {v4, v10}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-boolean v10, v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;->enable:Z

    if-nez v10, :cond_d

    goto :goto_5

    :cond_d
    new-instance v10, Ljava/lang/StringBuilder;

    const-string v11, "featureInfo  MATCH for "

    invoke-direct {v10, v11}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v10, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v10

    invoke-static {v4, v10}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v7}, Lmiui/contentcatcher/sdk/data/FeatureInfo;->getCatcherNameList()Ljava/util/List;

    move-result-object v10

    if-nez v10, :cond_e

    new-instance v10, Ljava/lang/StringBuilder;

    const-string v11, "None catchers found in featureInfo "

    invoke-direct {v10, v11}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v10, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-static {v4, v7}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_5

    :cond_e
    invoke-interface {v10}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v10

    const/4 v11, 0x0

    :goto_6
    invoke-interface {v10}, Ljava/util/Iterator;->hasNext()Z

    move-result v12

    if-eqz v12, :cond_16

    invoke-interface {v10}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v12

    check-cast v12, Ljava/lang/String;

    new-instance v13, Ljava/lang/StringBuilder;

    invoke-direct {v13}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v13, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v14, " in featureInfo"

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v13}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v13

    invoke-static {v4, v13}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {v12}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v13

    if-eqz v13, :cond_10

    :cond_f
    const/4 v14, 0x0

    goto :goto_7

    :cond_10
    iget-object v13, v1, Ld/c;->a:Ljava/io/Serializable;

    check-cast v13, Ljava/util/ArrayList;

    invoke-virtual {v13}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v13

    :cond_11
    invoke-interface {v13}, Ljava/util/Iterator;->hasNext()Z

    move-result v14

    if-eqz v14, :cond_f

    invoke-interface {v13}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v14

    check-cast v14, Lr/b;

    invoke-virtual {v14}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v15

    invoke-virtual {v15}, Ljava/lang/Class;->getSimpleName()Ljava/lang/String;

    move-result-object v15

    invoke-virtual {v12, v15}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v15

    if-eqz v15, :cond_11

    :goto_7
    if-nez v14, :cond_12

    invoke-static {v12, v9, v0}, Lr/a;->a(Ljava/lang/String;Landroid/app/Activity;Lcom/miui/contentcatcher/Interceptor;)Lr/b;

    move-result-object v14

    if-eqz v14, :cond_12

    iget-object v12, v1, Ld/c;->a:Ljava/io/Serializable;

    check-cast v12, Ljava/util/ArrayList;

    invoke-virtual {v12, v14}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_12
    if-eqz v14, :cond_15

    const-string v11, "parseFeatureInfoParams"

    invoke-static {v8, v11}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v11, v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;->target:Ljava/lang/String;

    iget-object v12, v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;->jobTag:Ljava/lang/String;

    iget-object v13, v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;->name:Ljava/lang/String;

    invoke-static {v11, v12, v13}, Lp/a;->a(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v11

    new-instance v12, Lt/a;

    const/4 v13, 0x4

    invoke-direct {v12, v11, v13}, Lt/a;-><init>(Ljava/lang/String;I)V

    const/4 v11, 0x2

    iput v11, v12, Lt/a;->d:I

    iget-object v11, v14, Lr/b;->b:Ljava/util/ArrayList;

    invoke-virtual {v11, v12}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    iget-object v11, v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;->mParams:Ljava/util/HashMap;

    const-string v12, "extra"

    invoke-virtual {v11, v12}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z

    move-result v11

    if-eqz v11, :cond_14

    iget-object v11, v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;->mParams:Ljava/util/HashMap;

    invoke-virtual {v11, v12}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v11

    check-cast v11, Ljava/util/Map;

    new-instance v12, Ljava/lang/StringBuilder;

    invoke-direct {v12}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual/range {p0 .. p0}, Lcom/miui/contentcatcher/Interceptor;->i()Ljava/lang/String;

    move-result-object v13

    invoke-virtual {v12, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v13, "/"

    invoke-virtual {v12, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual/range {p0 .. p0}, Lcom/miui/contentcatcher/Interceptor;->g()Ljava/lang/String;

    move-result-object v13

    invoke-virtual {v12, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v12}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v12

    invoke-interface {v11, v12}, Ljava/util/Map;->containsKey(Ljava/lang/Object;)Z

    move-result v13

    if-eqz v13, :cond_14

    invoke-interface {v11, v12}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v11

    iput-object v11, v14, Lr/b;->g:Ljava/lang/Object;

    if-eqz v11, :cond_14

    instance-of v11, v11, Ljava/util/Set;

    if-eqz v11, :cond_14

    new-instance v11, Ljava/util/HashSet;

    invoke-direct {v11}, Ljava/util/HashSet;-><init>()V

    iput-object v11, v14, Lr/b;->i:Ljava/util/HashSet;

    iget-object v11, v14, Lr/b;->g:Ljava/lang/Object;

    check-cast v11, Ljava/util/Set;

    invoke-interface {v11}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v11

    :goto_8
    invoke-interface {v11}, Ljava/util/Iterator;->hasNext()Z

    move-result v12

    if-eqz v12, :cond_14

    invoke-interface {v11}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v12

    check-cast v12, Ljava/lang/String;

    new-instance v13, Lcom/google/gson/d;

    invoke-direct {v13}, Lcom/google/gson/d;-><init>()V

    new-instance v15, Li/a;

    const-class v5, Lcom/miui/contentcatcher/common/ExtraConfigInfo;

    invoke-direct {v15, v5}, Li/a;-><init>(Ljava/lang/reflect/Type;)V

    if-nez v12, :cond_13

    move-object/from16 v17, v2

    const/4 v2, 0x0

    goto :goto_9

    :cond_13
    move-object/from16 v17, v2

    new-instance v2, Ljava/io/StringReader;

    invoke-direct {v2, v12}, Ljava/io/StringReader;-><init>(Ljava/lang/String;)V

    invoke-virtual {v13, v2, v15}, Lcom/google/gson/d;->b(Ljava/io/Reader;Li/a;)Ljava/lang/Object;

    move-result-object v2

    :goto_9
    invoke-static {v5}, Ld/a;->d(Ljava/lang/Class;)Ljava/lang/Class;

    move-result-object v5

    invoke-virtual {v5, v2}, Ljava/lang/Class;->cast(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lcom/miui/contentcatcher/common/ExtraConfigInfo;

    iget-object v5, v14, Lr/b;->i:Ljava/util/HashSet;

    invoke-virtual {v5, v2}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    move-object/from16 v2, v17

    goto :goto_8

    :cond_14
    move-object/from16 v17, v2

    const/4 v11, 0x1

    goto :goto_a

    :cond_15
    move-object/from16 v17, v2

    :goto_a
    move-object/from16 v2, v17

    goto/16 :goto_6

    :cond_16
    move-object/from16 v17, v2

    iget-object v2, v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;->name:Ljava/lang/String;

    const-string v5, "pick_mode"

    invoke-virtual {v5, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_18

    iget-object v2, v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;->mParams:Ljava/util/HashMap;

    const-string v10, "reflectioninfo"

    invoke-virtual {v2, v10}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_17

    iget-object v2, v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;->mParams:Ljava/util/HashMap;

    invoke-virtual {v2, v10}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/util/Map;

    goto :goto_b

    :cond_17
    const/4 v2, 0x0

    :goto_b
    iget-object v10, v0, Lcom/miui/contentcatcher/Interceptor;->r:Lmiui/contentcatcher/IViewContentFetcher;

    if-eqz v10, :cond_18

    invoke-interface {v10, v3, v2}, Lmiui/contentcatcher/IViewContentFetcher;->createFetcher(Lmiui/contentcatcher/sdk/Token;Ljava/util/Map;)V

    :cond_18
    iget-object v2, v0, Lcom/miui/contentcatcher/Interceptor;->d:Lu/a;

    if-nez v2, :cond_1b

    if-eqz v11, :cond_1a

    iget-object v2, v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;->name:Ljava/lang/String;

    invoke-virtual {v5, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_1a

    const/4 v2, 0x1

    iput-boolean v2, v0, Lcom/miui/contentcatcher/Interceptor;->l:Z

    iput-boolean v2, v0, Lcom/miui/contentcatcher/Interceptor;->n:Z

    iget v5, v0, Lcom/miui/contentcatcher/Interceptor;->q:I

    or-int/2addr v5, v2

    iput v5, v0, Lcom/miui/contentcatcher/Interceptor;->q:I

    :cond_19
    :goto_c
    const/4 v7, 0x2

    goto :goto_d

    :cond_1a
    const/4 v2, 0x1

    const-string v5, "image_pick_mode"

    iget-object v7, v7, Lmiui/contentcatcher/sdk/data/FeatureInfo;->name:Ljava/lang/String;

    invoke-virtual {v5, v7}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_19

    iput-boolean v2, v0, Lcom/miui/contentcatcher/Interceptor;->m:Z

    iput-boolean v2, v0, Lcom/miui/contentcatcher/Interceptor;->n:Z

    iget v5, v0, Lcom/miui/contentcatcher/Interceptor;->q:I

    const/4 v7, 0x2

    or-int/2addr v5, v7

    iput v5, v0, Lcom/miui/contentcatcher/Interceptor;->q:I

    goto :goto_d

    :cond_1b
    const/4 v2, 0x1

    goto :goto_c

    :goto_d
    move-object/from16 v2, v17

    goto/16 :goto_5

    :cond_1c
    const/4 v2, 0x1

    goto :goto_f

    :goto_e
    new-instance v3, Ljava/lang/StringBuilder;

    const-string v5, "Feature list invalid: "

    invoke-direct {v3, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v4, v3}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    :goto_f
    invoke-virtual {v1}, Ld/c;->c()Z

    move-result v3

    if-nez v3, :cond_1d

    const-string v0, "none catchers"

    invoke-static {v4, v0}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    return-void

    :cond_1d
    iget-object v3, v1, Ld/c;->a:Ljava/io/Serializable;

    check-cast v3, Ljava/util/ArrayList;

    invoke-virtual {v3}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v3

    :goto_10
    invoke-interface {v3}, Ljava/util/Iterator;->hasNext()Z

    move-result v5

    if-eqz v5, :cond_1e

    invoke-interface {v3}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Lr/b;

    invoke-virtual {v5}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    goto :goto_10

    :cond_1e
    iget-object v3, v1, Ld/c;->a:Ljava/io/Serializable;

    check-cast v3, Ljava/util/ArrayList;

    invoke-virtual {v3}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v3

    :goto_11
    invoke-interface {v3}, Ljava/util/Iterator;->hasNext()Z

    move-result v5

    if-eqz v5, :cond_1f

    invoke-interface {v3}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Lr/b;

    invoke-virtual {v5}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    goto :goto_11

    :cond_1f
    iget-object v3, v1, Ld/c;->a:Ljava/io/Serializable;

    check-cast v3, Ljava/util/ArrayList;

    invoke-virtual {v3}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v3

    :goto_12
    invoke-interface {v3}, Ljava/util/Iterator;->hasNext()Z

    move-result v5

    if-eqz v5, :cond_20

    invoke-interface {v3}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Lr/b;

    invoke-virtual {v5}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    goto :goto_12

    :cond_20
    iget-object v3, v1, Ld/c;->a:Ljava/io/Serializable;

    check-cast v3, Ljava/util/ArrayList;

    invoke-virtual {v3}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v3

    :goto_13
    invoke-interface {v3}, Ljava/util/Iterator;->hasNext()Z

    move-result v5

    if-eqz v5, :cond_21

    invoke-interface {v3}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Lr/b;

    invoke-virtual {v5}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    goto :goto_13

    :cond_21
    iget-object v1, v1, Ld/c;->a:Ljava/io/Serializable;

    check-cast v1, Ljava/util/ArrayList;

    invoke-virtual {v1}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :goto_14
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v3

    if-eqz v3, :cond_22

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Lr/b;

    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    goto :goto_14

    :cond_22
    iget-boolean v1, v0, Lcom/miui/contentcatcher/Interceptor;->l:Z

    if-nez v1, :cond_23

    iget-boolean v1, v0, Lcom/miui/contentcatcher/Interceptor;->m:Z

    if-nez v1, :cond_23

    iget-boolean v1, v0, Lcom/miui/contentcatcher/Interceptor;->n:Z

    if-eqz v1, :cond_27

    :cond_23
    invoke-static {v6}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v1

    if-nez v1, :cond_24

    const-string v1, "cts"

    invoke-virtual {v6, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v1

    if-eqz v1, :cond_24

    const/4 v7, 0x0

    goto :goto_15

    :cond_24
    move v7, v2

    :goto_15
    new-instance v1, Ljava/lang/StringBuilder;

    const-string v3, "needCancelLongClick "

    invoke-direct {v1, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v3, " enable:"

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1, v7}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v4, v1}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v8, Lu/a;

    iget-boolean v1, v0, Lcom/miui/contentcatcher/Interceptor;->l:Z

    if-nez v1, :cond_26

    iget-boolean v1, v0, Lcom/miui/contentcatcher/Interceptor;->m:Z

    if-eqz v1, :cond_25

    goto :goto_16

    :cond_25
    const/4 v5, 0x0

    goto :goto_17

    :cond_26
    :goto_16
    move v5, v2

    :goto_17
    iget-boolean v6, v0, Lcom/miui/contentcatcher/Interceptor;->n:Z

    iget-object v4, v0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    move-object v1, v8

    move-object v2, v4

    move-object/from16 v3, p0

    invoke-direct/range {v1 .. v7}, Lu/a;-><init>(Landroid/content/Context;Lcom/miui/contentcatcher/Interceptor;Landroid/app/Activity;ZZZ)V

    iput-object v8, v0, Lcom/miui/contentcatcher/Interceptor;->d:Lu/a;

    :cond_27
    return-void

    :cond_28
    :goto_18
    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "NOT interest in this page: "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v4, v0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    return-void
.end method

.method public final notifyActivityCreate()V
    .locals 5

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    const-string v1, "Interceptor_Catcher"

    :try_start_0
    const-string v2, "notifyActivityCreate: "

    invoke-static {v1, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    new-instance v2, Lq/a;

    invoke-direct {v2}, Lmiui/contentcatcher/sdk/injector/IContentDecorateCallback$Stub;-><init>()V

    new-instance v3, Ljava/lang/ref/WeakReference;

    invoke-direct {v3, p0}, Ljava/lang/ref/WeakReference;-><init>(Ljava/lang/Object;)V

    iput-object v3, v2, Lq/a;->a:Ljava/lang/ref/WeakReference;

    iput-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->f:Lq/a;

    invoke-static {}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->getInstance()Lmiui/contentcatcher/sdk/ContentCatcherManager;

    move-result-object v2

    iget-object v3, p0, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;

    iget-object v4, p0, Lcom/miui/contentcatcher/Interceptor;->f:Lq/a;

    invoke-virtual {v2, v3, v4}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->registerContentInjector(Lmiui/contentcatcher/sdk/Token;Lmiui/contentcatcher/sdk/injector/IContentDecorateCallback;)V

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->n()V

    invoke-virtual {v0}, Ld/c;->c()Z

    move-result p0

    if-eqz p0, :cond_0

    iget-object p0, v0, Ld/c;->a:Ljava/io/Serializable;

    check-cast p0, Ljava/util/ArrayList;

    invoke-virtual {p0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :goto_0
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lr/b;

    invoke-virtual {v0}, Lr/b;->j()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p0

    goto :goto_1

    :catch_1
    move-exception p0

    goto :goto_2

    :goto_1
    const-string v0, "notifyActivityCreate-Error: "

    invoke-static {v0, p0, v1}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_3

    :goto_2
    const-string v0, "notifyActivityCreate-Exception: "

    invoke-static {v0, p0, v1}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_0
    :goto_3
    return-void
.end method

.method public final notifyActivityDestroy()V
    .locals 4

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    const-string v1, "Interceptor_Catcher"

    const-string v2, "notifyActivityDestroy: "

    :try_start_0
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->b:Ljava/lang/String;

    invoke-virtual {v3, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0}, Ld/c;->c()Z

    move-result v2

    if-eqz v2, :cond_0

    invoke-virtual {v0}, Ld/c;->e()V

    goto :goto_0

    :catch_0
    move-exception p0

    goto :goto_1

    :catch_1
    move-exception p0

    goto :goto_2

    :cond_0
    :goto_0
    invoke-static {}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->getInstance()Lmiui/contentcatcher/sdk/ContentCatcherManager;

    move-result-object v0

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;

    invoke-virtual {v0, v2}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->unregisterContentInjector(Lmiui/contentcatcher/sdk/Token;)V

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->b()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_3

    :goto_1
    const-string v0, "notifyActivityDestroy-Error: "

    invoke-static {v0, p0, v1}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_3

    :goto_2
    const-string v0, "notifyActivityDestroy-Exception: "

    invoke-static {v0, p0, v1}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :goto_3
    return-void
.end method

.method public final notifyActivityPause()V
    .locals 4

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    const-string v1, "Interceptor_Catcher"

    const-string v2, "notifyActivityPause: "

    :try_start_0
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->b:Ljava/lang/String;

    invoke-virtual {v3, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0}, Ld/c;->d()Z

    move-result v2

    if-eqz v2, :cond_0

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->m()V

    goto :goto_0

    :catch_0
    move-exception p0

    goto :goto_2

    :catch_1
    move-exception p0

    goto :goto_3

    :cond_0
    :goto_0
    invoke-virtual {v0}, Ld/c;->c()Z

    move-result p0

    if-eqz p0, :cond_1

    iget-object p0, v0, Ld/c;->a:Ljava/io/Serializable;

    check-cast p0, Ljava/util/ArrayList;

    invoke-virtual {p0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :goto_1
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_1

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lr/b;

    invoke-virtual {v0}, Lr/b;->l()V

    const/4 v2, 0x0

    iput-object v2, v0, Lr/b;->c:Lcom/miui/contentcatcher/Interceptor;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_1

    :goto_2
    const-string v0, "notifyActivityPause-Error: "

    invoke-static {v0, p0, v1}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_4

    :goto_3
    const-string v0, "notifyActivityPause-Exception: "

    invoke-static {v0, p0, v1}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_1
    :goto_4
    return-void
.end method

.method public final notifyActivityResume()V
    .locals 11

    const-string v0, "Interceptor_Catcher"

    const-string v1, "notifyActivityResume: "

    const/4 v2, 0x0

    :try_start_0
    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->o()V

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->b:Ljava/lang/String;

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v0, v1}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    iget-boolean v1, p0, Lcom/miui/contentcatcher/Interceptor;->k:Z

    if-eqz v1, :cond_0

    iput-boolean v2, p0, Lcom/miui/contentcatcher/Interceptor;->k:Z

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->n()V

    goto :goto_0

    :catch_0
    move-exception v1

    goto :goto_2

    :catch_1
    move-exception v1

    goto :goto_3

    :cond_0
    :goto_0
    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    invoke-virtual {v1}, Ld/c;->c()Z

    move-result v1

    if-eqz v1, :cond_1

    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    iget-object v1, v1, Ld/c;->a:Ljava/io/Serializable;

    check-cast v1, Ljava/util/ArrayList;

    invoke-virtual {v1}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :goto_1
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v3

    if-eqz v3, :cond_1

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Lr/b;

    iput-object p0, v3, Lr/b;->c:Lcom/miui/contentcatcher/Interceptor;

    invoke-virtual {v3}, Lr/b;->m()V

    goto :goto_1

    :cond_1
    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    invoke-virtual {v1}, Ld/c;->d()Z

    move-result v1

    if-eqz v1, :cond_2

    const-string v1, "notifyContentLoaded"

    invoke-static {v0, v1}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->y:Landroid/os/Handler;

    const/4 v3, 0x2

    const-wide/16 v4, 0x7d0

    invoke-virtual {v1, v3, v4, v5}, Landroid/os/Handler;->sendEmptyMessageDelayed(IJ)Z
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_4

    :goto_2
    const-string v3, "notifyActivityResume-Error: "

    invoke-static {v3, v1, v0}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_4

    :goto_3
    const-string v3, "notifyActivityResume-Exception: "

    invoke-static {v3, v1, v0}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_2
    :goto_4
    iput-boolean v2, p0, Lcom/miui/contentcatcher/Interceptor;->j:Z

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    if-eqz v0, :cond_c

    invoke-virtual {v0}, Landroid/app/Activity;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v0

    const-string v1, "com.miui.voiceassist.preferences.screenhelper"

    invoke-static {v0, v1, v2}, Landroid/provider/Settings$Global;->getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I

    move-result v0

    const/4 v1, 0x1

    if-ne v0, v1, :cond_3

    iput-boolean v1, p0, Lcom/miui/contentcatcher/Interceptor;->j:Z

    :cond_3
    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->d:Lu/a;

    if-eqz v0, :cond_c

    invoke-static {}, Landroid/os/UserHandle;->myUserId()I

    move-result v0

    const/16 v3, 0x3e7

    const-string v4, "support_long_press_mode"

    if-ne v0, v3, :cond_4

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->d:Lu/a;

    iget-object v3, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    invoke-virtual {v3}, Landroid/app/Activity;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v3

    invoke-static {v3, v4, v1}, Landroid/provider/Settings$Global;->getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I

    move-result v3

    iput v3, v0, Lu/a;->f:I

    goto :goto_5

    :cond_4
    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->d:Lu/a;

    iget-object v3, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    invoke-virtual {v3}, Landroid/app/Activity;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v3

    invoke-static {v3, v4, v1}, Landroid/provider/Settings$System;->getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I

    move-result v3

    iput v3, v0, Lu/a;->f:I

    :goto_5
    iget-boolean v0, p0, Lcom/miui/contentcatcher/Interceptor;->l:Z

    if-nez v0, :cond_5

    iget-boolean v0, p0, Lcom/miui/contentcatcher/Interceptor;->m:Z

    if-eqz v0, :cond_c

    :cond_5
    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->d:Lu/a;

    iget-object v3, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    invoke-virtual {v3}, Landroid/app/Activity;->getApplicationContext()Landroid/content/Context;

    move-result-object v3

    invoke-static {v3}, Lt/b;->a(Landroid/content/Context;)Lt/b;

    move-result-object v3

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->i()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->g()Ljava/lang/String;

    move-result-object p0

    iget-object v5, v3, Lt/b;->d:Ljava/util/ArrayList;

    invoke-virtual {v5}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v6

    iget-object v7, v3, Lt/b;->b:Ljava/util/ArrayList;

    const-string v8, "ContentCatcher_TaplusBlackListHelper"

    if-eqz v6, :cond_7

    const-string v6, " mActivities isEmpty"

    invoke-static {v8, v6}, Landroid/util/Slog;->i(Ljava/lang/String;Ljava/lang/String;)I

    iget-object v6, v3, Lt/b;->a:Landroid/content/Context;

    invoke-virtual {v6}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v6

    const-string v9, "taplus_blacklist"

    invoke-static {v6, v9}, Landroid/provider/Settings$Global;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v6

    iput-object v6, v3, Lt/b;->c:Ljava/lang/String;

    invoke-virtual {v7}, Ljava/util/ArrayList;->clear()V

    invoke-static {v6}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v10

    if-eqz v10, :cond_6

    const-string v6, " blacklist is empty"

    invoke-static {v8, v6}, Landroid/util/Slog;->i(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_7

    :cond_6
    :try_start_1
    new-instance v10, Lorg/json/JSONObject;

    invoke-direct {v10, v6}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V

    invoke-virtual {v10, v9}, Lorg/json/JSONObject;->getJSONArray(Ljava/lang/String;)Lorg/json/JSONArray;

    move-result-object v6

    move v9, v2

    :goto_6
    invoke-virtual {v6}, Lorg/json/JSONArray;->length()I

    move-result v10

    if-ge v9, v10, :cond_7

    invoke-virtual {v6, v9}, Lorg/json/JSONArray;->getString(I)Ljava/lang/String;

    move-result-object v10

    invoke-virtual {v7, v10}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    :try_end_1
    .catch Lorg/json/JSONException; {:try_start_1 .. :try_end_1} :catch_2

    add-int/lit8 v9, v9, 0x1

    goto :goto_6

    :catch_2
    move-exception v6

    invoke-virtual {v6}, Ljava/lang/Throwable;->printStackTrace()V

    :cond_7
    :goto_7
    invoke-virtual {v5, p0}, Ljava/util/ArrayList;->contains(Ljava/lang/Object;)Z

    move-result v6

    if-nez v6, :cond_8

    invoke-virtual {v5, p0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_8
    const-string p0, "com.miui.personalassistant"

    invoke-virtual {p0, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_9

    goto :goto_8

    :cond_9
    iget-object p0, v3, Lt/b;->c:Ljava/lang/String;

    invoke-static {p0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result p0

    if-eqz p0, :cond_a

    const-string p0, " blacklist is null mBlackListJson"

    invoke-static {v8, p0}, Landroid/util/Slog;->i(Ljava/lang/String;Ljava/lang/String;)I

    move v2, v1

    goto :goto_8

    :cond_a
    invoke-static {v4}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result p0

    if-eqz p0, :cond_b

    const-string p0, " pkg is null"

    invoke-static {v8, p0}, Landroid/util/Slog;->i(Ljava/lang/String;Ljava/lang/String;)I

    :cond_b
    invoke-virtual {v7, v4}, Ljava/util/ArrayList;->contains(Ljava/lang/Object;)Z

    move-result p0

    xor-int/lit8 v2, p0, 0x1

    new-instance p0, Ljava/lang/StringBuilder;

    const-string v1, " mActivities : "

    invoke-direct {p0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v1, " Blacklist json : "

    invoke-virtual {p0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v8, p0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p0, Ljava/lang/StringBuilder;

    const-string v1, " blackList size: "

    invoke-direct {p0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v7}, Ljava/util/ArrayList;->size()I

    move-result v1

    invoke-virtual {p0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v1, " package is "

    invoke-virtual {p0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, " isInTaplusWhiteList: "

    invoke-virtual {p0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0, v2}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v8, p0}, Landroid/util/Slog;->i(Ljava/lang/String;Ljava/lang/String;)I

    :goto_8
    iput-boolean v2, v0, Lu/a;->r:Z

    :cond_c
    return-void
.end method

.method public final notifyActivityStart()V
    .locals 4

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    const-string v1, "Interceptor_Catcher"

    const-string v2, "notifyActivityStart: "

    :try_start_0
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->b:Ljava/lang/String;

    invoke-virtual {v3, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v1, p0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0}, Ld/c;->c()Z

    move-result p0

    if-eqz p0, :cond_0

    iget-object p0, v0, Ld/c;->a:Ljava/io/Serializable;

    check-cast p0, Ljava/util/ArrayList;

    invoke-virtual {p0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :goto_0
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lr/b;

    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p0

    goto :goto_1

    :catch_1
    move-exception p0

    goto :goto_2

    :goto_1
    const-string v0, "notifyActivityStart-Error: "

    invoke-static {v0, p0, v1}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_3

    :goto_2
    const-string v0, "notifyActivityStart-Exception: "

    invoke-static {v0, p0, v1}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_0
    :goto_3
    return-void
.end method

.method public final notifyActivityStop()V
    .locals 4

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    const-string v1, "Interceptor_Catcher"

    const-string v2, "notifyActivityStop: "

    :try_start_0
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->b:Ljava/lang/String;

    invoke-virtual {v3, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0}, Ld/c;->c()Z

    move-result v2

    if-eqz v2, :cond_0

    iget-object v0, v0, Ld/c;->a:Ljava/io/Serializable;

    check-cast v0, Ljava/util/ArrayList;

    invoke-virtual {v0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :goto_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_0

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lr/b;

    invoke-virtual {v2}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    goto :goto_0

    :cond_0
    iget-boolean v0, p0, Lcom/miui/contentcatcher/Interceptor;->l:Z

    if-nez v0, :cond_1

    iget-boolean v0, p0, Lcom/miui/contentcatcher/Interceptor;->m:Z

    if-eqz v0, :cond_2

    goto :goto_1

    :catch_0
    move-exception p0

    goto :goto_2

    :catch_1
    move-exception p0

    goto :goto_3

    :cond_1
    :goto_1
    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    if-eqz v0, :cond_2

    invoke-virtual {v0}, Landroid/app/Activity;->getApplicationContext()Landroid/content/Context;

    move-result-object v0

    invoke-static {v0}, Lt/b;->a(Landroid/content/Context;)Lt/b;

    move-result-object v0

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->g()Ljava/lang/String;

    move-result-object p0

    iget-object v0, v0, Lt/b;->d:Ljava/util/ArrayList;

    invoke-virtual {v0, p0}, Ljava/util/ArrayList;->remove(Ljava/lang/Object;)Z
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_4

    :goto_2
    const-string v0, "notifyActivityStop-Error: "

    invoke-static {v0, p0, v1}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_4

    :goto_3
    const-string v0, "notifyActivityStop-Exception: "

    invoke-static {v0, p0, v1}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_2
    :goto_4
    return-void
.end method

.method public final notifyWebView(Landroid/view/View;Z)V
    .locals 2

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->y:Landroid/os/Handler;

    new-instance v1, Lcom/miui/contentcatcher/Interceptor$b;

    invoke-direct {v1, p0, p2, p1}, Lcom/miui/contentcatcher/Interceptor$b;-><init>(Lcom/miui/contentcatcher/Interceptor;ZLandroid/view/View;)V

    invoke-virtual {v0, v1}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    return-void
.end method

.method public final o()V
    .locals 8

    const-string v0, "extra key="

    const-string v1, "Interceptor_Catcher"

    const-string v2, "intent.getAction():"

    const-string v3, "intent.getDataString():"

    const-string v4, "intent: "

    const-string v5, "mTokenString: "

    :try_start_0
    sget-boolean v6, Lp/b;->a:Z

    if-eqz v6, :cond_2

    iget-object v6, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    if-eqz v6, :cond_2

    invoke-virtual {v6}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;

    move-result-object v6

    const-string v7, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>begin print activity onCreate intent<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"

    invoke-static {v1, v7}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v7, Ljava/lang/StringBuilder;

    invoke-direct {v7, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->b:Ljava/lang/String;

    invoke-virtual {v7, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v1, p0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p0, Ljava/lang/StringBuilder;

    invoke-direct {p0, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v1, p0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    if-eqz v6, :cond_1

    new-instance p0, Ljava/lang/StringBuilder;

    invoke-direct {p0, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v6}, Landroid/content/Intent;->getDataString()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {p0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v1, p0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p0, Ljava/lang/StringBuilder;

    invoke-direct {p0, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v6}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {p0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v1, p0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v6}, Landroid/content/Intent;->getExtras()Landroid/os/Bundle;

    move-result-object p0

    if-eqz p0, :cond_0

    const-string v2, "extra is not null"

    invoke-static {v1, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    invoke-virtual {p0}, Landroid/os/Bundle;->keySet()Ljava/util/Set;

    move-result-object v2

    invoke-interface {v2}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v2

    :goto_0
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    move-result v3

    if-eqz v3, :cond_2

    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/String;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_2

    :try_start_1
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v4, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v5, ", value="

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    new-instance v5, Lcom/google/gson/d;

    invoke-direct {v5}, Lcom/google/gson/d;-><init>()V

    invoke-virtual {p0, v3}, Landroid/os/Bundle;->get(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v6

    invoke-virtual {v5, v6}, Lcom/google/gson/d;->f(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-static {v1, v4}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1
    .catch Ljava/lang/Error; {:try_start_1 .. :try_end_1} :catch_0

    goto :goto_0

    :catch_0
    move-exception v4

    goto :goto_1

    :catch_1
    move-exception v4

    goto :goto_2

    :goto_1
    :try_start_2
    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v5, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v3, ", Error="

    invoke-virtual {v5, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v1, v3}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_0

    :catch_2
    move-exception p0

    goto :goto_4

    :goto_2
    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v5, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v3, ", Exception="

    invoke-virtual {v5, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v1, v3}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_0

    :cond_0
    const-string p0, "extra is null"

    invoke-static {v1, p0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_3

    :cond_1
    const-string p0, "getIntent is null"

    invoke-static {v1, p0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    :cond_2
    :goto_3
    const-string p0, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>finish print activity onCreate intent<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"

    invoke-static {v1, p0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_2

    goto :goto_5

    :goto_4
    const-string v0, "printIntent error: "

    invoke-static {v0, p0, v1}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :goto_5
    return-void
.end method

.method public final onUiAgent(Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V
    .locals 8

    const-class v0, Lz/j;

    monitor-enter v0

    :try_start_0
    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->v:Lz/j;

    if-nez v1, :cond_1

    sget v1, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v2, 0x22

    if-lt v1, v2, :cond_0

    new-instance v1, Lz/j;

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->x:Landroid/os/Handler;

    invoke-virtual {v2}, Landroid/os/Handler;->getLooper()Landroid/os/Looper;

    move-result-object v2

    invoke-direct {v1, v2}, Lz/j;-><init>(Landroid/os/Looper;)V

    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->v:Lz/j;

    goto :goto_0

    :catchall_0
    move-exception p0

    goto/16 :goto_6

    :cond_0
    const-string p0, "SDK_INT < UPSIDE_DOWN_CAKE"

    const-string v1, ""

    invoke-static {p0, v1, p1, p2}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    monitor-exit v0

    return-void

    :cond_1
    :goto_0
    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->v:Lz/j;

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    const-string v1, "pushRequest"

    const-string v2, "Catcher_AICollectorManager"

    invoke-static {v2, v1}, Lp/b;->l(Ljava/lang/String;Ljava/lang/String;)V

    const-string v1, ""

    :try_start_1
    new-instance v3, Landroid/os/Bundle;

    invoke-direct {v3, p1}, Landroid/os/Bundle;-><init>(Landroid/os/Bundle;)V

    if-nez p0, :cond_2

    const-string p0, "context is null"

    invoke-static {p0, v1, v3, p2}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    goto/16 :goto_5

    :catchall_1
    move-exception p0

    goto/16 :goto_4

    :cond_2
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p0}, Landroid/app/Activity;->getPackageName()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v4, "~"

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Landroid/app/Activity;->getApplicationInfo()Landroid/content/pm/ApplicationInfo;

    move-result-object v4

    iget v4, v4, Landroid/content/pm/ApplicationInfo;->uid:I

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v3

    if-eqz v3, :cond_4

    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v3

    invoke-virtual {v3}, Landroid/view/Window;->peekDecorView()Landroid/view/View;

    move-result-object v3

    if-nez v3, :cond_3

    goto :goto_1

    :cond_3
    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v3

    invoke-virtual {v3}, Landroid/view/Window;->peekDecorView()Landroid/view/View;

    move-result-object v3

    goto :goto_2

    :cond_4
    :goto_1
    const/4 v3, 0x0

    :goto_2
    invoke-static {p0, v3, p1}, Lz/j;->g(Landroid/app/Activity;Landroid/view/View;Landroid/os/Bundle;)Ljava/lang/String;

    move-result-object v4

    if-eqz v4, :cond_5

    invoke-static {v4, v1, p1, p2}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    goto/16 :goto_5

    :cond_5
    new-instance v4, Lz/j$c;

    invoke-direct {v4}, Lz/j$c;-><init>()V

    new-instance v5, Ljava/lang/ref/WeakReference;

    invoke-direct {v5, v3}, Ljava/lang/ref/WeakReference;-><init>(Ljava/lang/Object;)V

    iput-object v5, v4, Lz/j$c;->b:Ljava/lang/ref/WeakReference;

    new-instance v5, Ljava/lang/ref/WeakReference;

    invoke-direct {v5, p0}, Ljava/lang/ref/WeakReference;-><init>(Ljava/lang/Object;)V

    iput-object v5, v4, Lz/j$c;->c:Ljava/lang/ref/WeakReference;

    iput-object p1, v4, Lz/j$c;->d:Landroid/os/Bundle;

    iput-object p2, v4, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    iput-object v1, v4, Lz/j$c;->a:Ljava/lang/String;

    invoke-virtual {p0}, Landroid/app/Activity;->getPackageName()Ljava/lang/String;

    move-result-object v5

    iput-object v5, v4, Lz/j$c;->f:Ljava/lang/String;

    const-string v5, "com.miui.contentcatcher"
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    const/4 v6, -0x1

    :try_start_2
    invoke-virtual {p0}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object p0

    const/16 v7, 0x80

    invoke-virtual {p0, v5, v7}, Landroid/content/pm/PackageManager;->getApplicationInfo(Ljava/lang/String;I)Landroid/content/pm/ApplicationInfo;

    move-result-object p0

    iget-object p0, p0, Landroid/content/pm/ApplicationInfo;->metaData:Landroid/os/Bundle;

    if-nez p0, :cond_6

    goto :goto_3

    :cond_6
    const-string v5, "agent_version"

    invoke-static {p0, v5, v6}, Lz/k;->e(Landroid/os/Bundle;Ljava/lang/String;I)I

    move-result v6
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_0
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    :catch_0
    :goto_3
    :try_start_3
    iput v6, v4, Lz/j$c;->g:I

    invoke-virtual {v3}, Landroid/view/View;->getDisplay()Landroid/view/Display;

    move-result-object p0

    new-instance v3, Landroid/graphics/Point;

    invoke-direct {v3}, Landroid/graphics/Point;-><init>()V

    invoke-virtual {p0, v3}, Landroid/view/Display;->getRealSize(Landroid/graphics/Point;)V

    invoke-virtual {p0}, Landroid/view/Display;->getRotation()I

    move-result p0

    iput p0, v4, Lz/j$c;->h:I

    new-instance p0, Landroid/graphics/Rect;

    iget v5, v3, Landroid/graphics/Point;->x:I

    iget v3, v3, Landroid/graphics/Point;->y:I

    const/4 v6, 0x0

    invoke-direct {p0, v6, v6, v5, v3}, Landroid/graphics/Rect;-><init>(IIII)V

    iput-object p0, v4, Lz/j$c;->i:Landroid/graphics/Rect;

    iget-object p0, v0, Lz/j;->a:Landroid/os/Handler;

    invoke-virtual {p0, v6}, Landroid/os/Handler;->obtainMessage(I)Landroid/os/Message;

    move-result-object p0

    iput-object v4, p0, Landroid/os/Message;->obj:Ljava/lang/Object;

    invoke-virtual {p0}, Landroid/os/Message;->sendToTarget()V
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_1

    goto :goto_5

    :goto_4
    new-instance v0, Ljava/lang/StringBuilder;

    const-string v3, "pushRequest exception: "

    invoke-direct {v0, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v2, p0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    const-string p0, "pushRequest exception"

    invoke-static {p0, v1, p1, p2}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    :goto_5
    return-void

    :goto_6
    :try_start_4
    monitor-exit v0
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    throw p0
.end method

.method public final p()V
    .locals 3

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->o:Ljava/util/LinkedList;

    invoke-virtual {p0}, Ljava/util/AbstractCollection;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :cond_0
    :goto_0
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_1

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/ref/WeakReference;

    invoke-virtual {v0}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/view/View;

    if-eqz v0, :cond_0

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "print webview: "

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "Interceptor_Catcher"

    invoke-static {v1, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0

    :cond_1
    return-void
.end method

.method public final processRequest(Landroid/net/Uri;)V
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->u:Ly/g;

    if-eqz p0, :cond_0

    invoke-virtual {p0, p1}, Ly/g;->d(Landroid/net/Uri;)V

    :cond_0
    return-void
.end method

.method public final declared-synchronized q()V
    .locals 4

    const-string v0, "setCurrentWebview mTouchWebView: "

    monitor-enter p0

    :try_start_0
    iget-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->o:Ljava/util/LinkedList;

    sget-boolean v2, Lp/b;->a:Z

    if-eqz v2, :cond_0

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->p()V

    goto :goto_0

    :catchall_0
    move-exception v0

    goto :goto_1

    :cond_0
    :goto_0
    if-eqz v1, :cond_1

    invoke-virtual {v1}, Ljava/util/LinkedList;->size()I

    move-result v2

    if-lez v2, :cond_1

    invoke-virtual {v1}, Ljava/util/LinkedList;->getLast()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/ref/WeakReference;

    invoke-virtual {v2}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Landroid/view/View;

    iput-object v2, p0, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    const-string v2, "Interceptor_Catcher"

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor;->p:Landroid/view/View;

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v0, ", web size:"

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/util/LinkedList;->size()I

    move-result v0

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v2, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    :cond_1
    monitor-exit p0

    return-void

    :goto_1
    :try_start_1
    monitor-exit p0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    throw v0
.end method

.method public final updateContent(Lmiui/contentcatcher/sdk/DecorateContentParam;)V
    .locals 2
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Landroid/os/RemoteException;
        }
    .end annotation

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "IContentDecorateCallback updateContent params "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "Interceptor_Catcher"

    invoke-static {v1, v0}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    if-nez p1, :cond_0

    iget-object p1, p0, Lcom/miui/contentcatcher/Interceptor;->a:Landroid/app/Activity;

    if-eqz p1, :cond_0

    const-string p1, "updateContent set mShouldUpdate"

    invoke-static {v1, p1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    const/4 p1, 0x1

    iput-boolean p1, p0, Lcom/miui/contentcatcher/Interceptor;->k:Z

    :cond_0
    return-void
.end method
