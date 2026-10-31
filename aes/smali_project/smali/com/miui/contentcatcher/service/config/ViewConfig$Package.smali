.class public Lcom/miui/contentcatcher/service/config/ViewConfig$Package;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/miui/contentcatcher/service/config/ViewConfig;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x9
    name = "Package"
.end annotation

.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;
    }
.end annotation


# instance fields
.field private configs:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;",
            ">;"
        }
    .end annotation
.end field

.field private packageName:Ljava/lang/String;
    .annotation runtime Lc/b;
        value = "package"
    .end annotation
.end field


# direct methods
.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public getConfig(I)Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;
    .locals 4

    iget-object v0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package;->configs:Ljava/util/List;

    const/4 v1, 0x0

    if-nez v0, :cond_0

    return-object v1

    :cond_0
    invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :cond_1
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_2

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;

    invoke-static {v2}, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;->a(Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;)I

    move-result v3

    if-ne v3, p1, :cond_1

    return-object v2

    :cond_2
    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package;->configs:Ljava/util/List;

    invoke-interface {p0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :cond_3
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_4

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;

    invoke-static {v0}, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;->a(Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;)I

    move-result v2

    if-ge v2, p1, :cond_3

    return-object v0

    :cond_4
    return-object v1
.end method

.method public getConfigs()Ljava/util/List;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/List<",
            "Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;",
            ">;"
        }
    .end annotation

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package;->configs:Ljava/util/List;

    return-object p0
.end method

.method public getPackageName()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package;->packageName:Ljava/lang/String;

    return-object p0
.end method

.method public setConfigs(Ljava/util/List;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List<",
            "Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;",
            ">;)V"
        }
    .end annotation

    iput-object p1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package;->configs:Ljava/util/List;

    return-void
.end method

.method public setPackageName(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package;->packageName:Ljava/lang/String;

    return-void
.end method

.method public toString()Ljava/lang/String;
    .locals 2

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "Package{packageName=\'"

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package;->packageName:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "\', configs="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package;->configs:Ljava/util/List;

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const/16 p0, 0x7d

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method
