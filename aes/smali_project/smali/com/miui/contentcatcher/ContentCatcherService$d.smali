.class public final Lcom/miui/contentcatcher/ContentCatcherService$d;
.super Landroid/database/ContentObserver;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/miui/contentcatcher/ContentCatcherService;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = "d"
.end annotation


# instance fields
.field public final synthetic a:Lcom/miui/contentcatcher/ContentCatcherService;


# direct methods
.method public constructor <init>(Lcom/miui/contentcatcher/ContentCatcherService;Landroid/os/Handler;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/ContentCatcherService$d;->a:Lcom/miui/contentcatcher/ContentCatcherService;

    invoke-direct {p0, p2}, Landroid/database/ContentObserver;-><init>(Landroid/os/Handler;)V

    return-void
.end method


# virtual methods
.method public final onChange(ZLandroid/net/Uri;)V
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService$d;->a:Lcom/miui/contentcatcher/ContentCatcherService;

    invoke-virtual {p0}, Lcom/miui/contentcatcher/ContentCatcherService;->a()V

    return-void
.end method
