.class public Lcom/miui/contentcatcher/service/config/ViewConfig;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/miui/contentcatcher/service/config/ViewConfig$Package;
    }
.end annotation


# instance fields
.field private configVersion:I
    .annotation runtime Lc/b;
        value = "config_version"
    .end annotation
.end field

.field private jobTag:Ljava/lang/String;
    .annotation runtime Lc/b;
        value = "job_tag"
    .end annotation
.end field

.field private packages:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lcom/miui/contentcatcher/service/config/ViewConfig$Package;",
            ">;"
        }
    .end annotation
.end field

.field private target:Ljava/lang/String;


# direct methods
.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public getConfigVersion()I
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->configVersion:I

    return p0
.end method

.method public getJobTag()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->jobTag:Ljava/lang/String;

    return-object p0
.end method

.method public getPackage(Ljava/lang/String;)Lcom/miui/contentcatcher/service/config/ViewConfig$Package;
    .locals 3

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->packages:Ljava/util/List;

    const/4 v0, 0x0

    if-eqz p0, :cond_2

    if-nez p1, :cond_0

    goto :goto_0

    :cond_0
    invoke-interface {p0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :cond_1
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_2

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lcom/miui/contentcatcher/service/config/ViewConfig$Package;

    invoke-virtual {v1}, Lcom/miui/contentcatcher/service/config/ViewConfig$Package;->getPackageName()Ljava/lang/String;

    move-result-object v2

    invoke-static {v2, p1}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z

    move-result v2

    if-eqz v2, :cond_1

    return-object v1

    :cond_2
    :goto_0
    return-object v0
.end method

.method public getPackages()Ljava/util/List;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/List<",
            "Lcom/miui/contentcatcher/service/config/ViewConfig$Package;",
            ">;"
        }
    .end annotation

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->packages:Ljava/util/List;

    return-object p0
.end method

.method public getTarget()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->target:Ljava/lang/String;

    return-object p0
.end method

.method public setConfigVersion(I)V
    .locals 0

    iput p1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->configVersion:I

    return-void
.end method

.method public setJobTag(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->jobTag:Ljava/lang/String;

    return-void
.end method

.method public setPackages(Ljava/util/List;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List<",
            "Lcom/miui/contentcatcher/service/config/ViewConfig$Package;",
            ">;)V"
        }
    .end annotation

    iput-object p1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->packages:Ljava/util/List;

    return-void
.end method

.method public setTarget(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->target:Ljava/lang/String;

    return-void
.end method

.method public toString()Ljava/lang/String;
    .locals 2

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "ViewConfig{target=\'"

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->target:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "\', jobTag=\'"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->jobTag:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "\', configVersion="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget v1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->configVersion:I

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v1, ", packages="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig;->packages:Ljava/util/List;

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const/16 p0, 0x7d

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method
