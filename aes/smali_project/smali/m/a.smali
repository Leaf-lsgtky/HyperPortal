.class public final Lm/a;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/lang/Runnable;


# instance fields
.field public final synthetic a:I

.field public final synthetic b:Ljava/lang/Object;


# direct methods
.method public synthetic constructor <init>(Ljava/lang/Object;I)V
    .locals 0

    iput p2, p0, Lm/a;->a:I

    iput-object p1, p0, Lm/a;->b:Ljava/lang/Object;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public final run()V
    .locals 6

    iget v0, p0, Lm/a;->a:I

    packed-switch v0, :pswitch_data_0

    iget-object p0, p0, Lm/a;->b:Ljava/lang/Object;

    check-cast p0, Landroid/content/Context;

    const-string v0, "MigrationPreferences"

    const/4 v1, 0x0

    invoke-virtual {p0, v0, v1}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;

    move-result-object v0

    const-string v2, "migration_attempt_count"

    invoke-interface {v0, v2, v1}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I

    move-result v3

    const/16 v4, 0xa

    if-ge v3, v4, :cond_0

    invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;

    move-result-object v0

    add-int/lit8 v3, v3, 0x1

    invoke-interface {v0, v2, v3}, Landroid/content/SharedPreferences$Editor;->putInt(Ljava/lang/String;I)Landroid/content/SharedPreferences$Editor;

    move-result-object v0

    invoke-interface {v0}, Landroid/content/SharedPreferences$Editor;->apply()V

    :cond_0
    const-string v0, "Data migration process started"

    const-string v2, "DATA_TRANSFERDataMigrationManager"

    invoke-static {v2, v0}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    :try_start_0
    invoke-static {p0}, Ln/c;->e(Landroid/content/Context;)Landroid/os/Bundle;

    move-result-object v0

    invoke-static {v0}, Ln/c;->i(Landroid/os/Bundle;)Ljava/security/PublicKey;

    move-result-object v3

    invoke-static {v0}, Ln/c;->h(Landroid/os/Bundle;)Ljava/lang/String;

    move-result-object v0

    invoke-static {}, Ln/c;->g()Ljavax/crypto/SecretKey;

    move-result-object v4

    invoke-static {p0, v4}, Ln/c;->d(Landroid/content/Context;Ljavax/crypto/SecretKey;)Ljava/io/File;

    move-result-object v5

    invoke-static {v4, v3}, Ln/c;->c(Ljavax/crypto/SecretKey;Ljava/security/PublicKey;)[B

    move-result-object v3

    invoke-static {p0, v5, v3, v0}, Ln/c;->j(Landroid/content/Context;Ljava/io/File;[BLjava/lang/String;)V
    :try_end_0
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_3
    .catch Ljava/lang/SecurityException; {:try_start_0 .. :try_end_0} :catch_2
    .catch Ljava/security/InvalidKeyException; {:try_start_0 .. :try_end_0} :catch_2
    .catch Ljava/security/NoSuchAlgorithmException; {:try_start_0 .. :try_end_0} :catch_2
    .catch Ljava/security/spec/InvalidKeySpecException; {:try_start_0 .. :try_end_0} :catch_2
    .catch Ljavax/crypto/NoSuchPaddingException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljavax/crypto/IllegalBlockSizeException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljavax/crypto/BadPaddingException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_4

    :catch_0
    move-exception p0

    goto :goto_0

    :catch_1
    move-exception p0

    goto :goto_1

    :catch_2
    move-exception p0

    goto :goto_2

    :catch_3
    move-exception p0

    goto :goto_3

    :goto_0
    const-string v0, "An unexpected error occurred during data migration"

    invoke-static {v2, v0, p0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    goto :goto_4

    :goto_1
    const-string v0, "Encryption operation err"

    invoke-static {v2, v0, p0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    goto :goto_4

    :goto_2
    const-string v0, "Key store err during data migration"

    invoke-static {v2, v0, p0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    goto :goto_4

    :goto_3
    const-string v0, "IO operation err during data migration"

    invoke-static {v2, v0, p0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    :goto_4
    sput-boolean v1, Ln/a;->b:Z

    return-void

    :pswitch_0
    iget-object v0, p0, Lm/a;->b:Ljava/lang/Object;

    check-cast v0, Lcom/miui/contentcatcher/Interceptor;

    monitor-enter v0

    :try_start_1
    iget-object p0, p0, Lm/a;->b:Ljava/lang/Object;

    check-cast p0, Lcom/miui/contentcatcher/Interceptor;

    const/4 v1, 0x0

    iput-object v1, p0, Lcom/miui/contentcatcher/Interceptor;->s:Lmiui/contentcatcher/sdk/Content;

    monitor-exit v0

    return-void

    :catchall_0
    move-exception p0

    monitor-exit v0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    throw p0

    nop

    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_0
    .end packed-switch
.end method
