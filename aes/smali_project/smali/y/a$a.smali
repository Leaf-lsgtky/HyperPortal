.class public final Ly/a$a;
.super LB/a;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Ly/a;->e(Landroid/content/Context;ILjava/util/ArrayList;Lcom/miui/contentcatcher/common/NodeInfo;)V
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = null
.end annotation


# instance fields
.field public final synthetic a:Lcom/miui/contentcatcher/common/NodeInfo;


# direct methods
.method public constructor <init>(Lcom/miui/contentcatcher/common/NodeInfo;)V
    .locals 0

    iput-object p1, p0, Ly/a$a;->a:Lcom/miui/contentcatcher/common/NodeInfo;

    invoke-direct {p0}, Landroid/os/Binder;-><init>()V

    const-string p1, "com.xiaomi.aicr.ICatcherInterface"

    invoke-virtual {p0, p0, p1}, Landroid/os/Binder;->attachInterface(Landroid/os/IInterface;Ljava/lang/String;)V

    return-void
.end method
