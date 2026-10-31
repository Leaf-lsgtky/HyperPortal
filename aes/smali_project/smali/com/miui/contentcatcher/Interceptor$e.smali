.class public final Lcom/miui/contentcatcher/Interceptor$e;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/lang/Runnable;


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/miui/contentcatcher/Interceptor;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = "e"
.end annotation


# instance fields
.field public a:Ljava/util/HashMap;

.field public b:Landroid/os/Bundle;

.field public final synthetic c:Lcom/miui/contentcatcher/Interceptor;


# direct methods
.method public constructor <init>(Lcom/miui/contentcatcher/Interceptor;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/miui/contentcatcher/Interceptor$e;->c:Lcom/miui/contentcatcher/Interceptor;

    return-void
.end method


# virtual methods
.method public final run()V
    .locals 5

    iget-object v0, p0, Lcom/miui/contentcatcher/Interceptor$e;->c:Lcom/miui/contentcatcher/Interceptor;

    const/4 v1, 0x0

    :try_start_0
    iget-boolean v2, v0, Lcom/miui/contentcatcher/Interceptor;->i:Z

    if-nez v2, :cond_0

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor$e;->b:Landroid/os/Bundle;

    if-eqz v2, :cond_0

    iget-object v3, p0, Lcom/miui/contentcatcher/Interceptor$e;->a:Ljava/util/HashMap;

    if-eqz v3, :cond_0

    iget-object v3, v0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    invoke-virtual {v3, v2}, Ld/c;->f(Landroid/os/Bundle;)V

    iget-object v2, p0, Lcom/miui/contentcatcher/Interceptor$e;->a:Ljava/util/HashMap;

    const-string v3, "isedittext"

    const-string v4, "0"

    invoke-virtual {v2, v3, v4}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    iget-object v2, v0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    iget-object v0, v0, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;

    iget-object v3, p0, Lcom/miui/contentcatcher/Interceptor$e;->a:Ljava/util/HashMap;

    const/4 v4, 0x4

    invoke-virtual {v2, v4, v0, v3}, Ld/c;->b(ILmiui/contentcatcher/sdk/Token;Ljava/util/HashMap;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    goto :goto_0

    :catchall_0
    move-exception v0

    goto :goto_1

    :cond_0
    :goto_0
    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor$e;->b:Landroid/os/Bundle;

    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor$e;->a:Ljava/util/HashMap;

    return-void

    :goto_1
    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor$e;->b:Landroid/os/Bundle;

    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor$e;->a:Ljava/util/HashMap;

    throw v0
.end method
