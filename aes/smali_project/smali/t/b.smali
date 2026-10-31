.class public final Lt/b;
.super Ljava/lang/Object;
.source "SourceFile"


# static fields
.field public static volatile e:Lt/b;


# instance fields
.field public final a:Landroid/content/Context;

.field public final b:Ljava/util/ArrayList;

.field public c:Ljava/lang/String;

.field public final d:Ljava/util/ArrayList;


# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    iput-object v0, p0, Lt/b;->b:Ljava/util/ArrayList;

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    iput-object v0, p0, Lt/b;->d:Ljava/util/ArrayList;

    iput-object p1, p0, Lt/b;->a:Landroid/content/Context;

    return-void
.end method

.method public static a(Landroid/content/Context;)Lt/b;
    .locals 2

    sget-object v0, Lt/b;->e:Lt/b;

    if-nez v0, :cond_1

    const-class v0, Lt/b;

    monitor-enter v0

    :try_start_0
    sget-object v1, Lt/b;->e:Lt/b;

    if-nez v1, :cond_0

    new-instance v1, Lt/b;

    invoke-direct {v1, p0}, Lt/b;-><init>(Landroid/content/Context;)V

    sput-object v1, Lt/b;->e:Lt/b;

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
    sget-object p0, Lt/b;->e:Lt/b;

    return-object p0
.end method
