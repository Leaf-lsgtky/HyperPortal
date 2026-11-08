.class public Lcom/miui/contentcatcher/ContentCatcherService;
.super Lmiui/contentcatcher/IContentCatcherService$Stub;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/miui/contentcatcher/ContentCatcherService$c;,
        Lcom/miui/contentcatcher/ContentCatcherService$d;
    }
.end annotation


# instance fields
.field public final a:Lw/a;

.field public final b:Landroid/content/Context;

.field public final c:Landroid/content/ClipboardManager;

.field public d:I

.field public final e:Ljava/util/HashSet;

.field public final f:Ljava/util/HashSet;

.field public final g:Landroid/content/IClipboard;

.field public final h:Lcom/miui/contentcatcher/ContentCatcherService$a;

.field public final i:Lcom/miui/contentcatcher/ContentCatcherService$c;

.field public final j:Lcom/miui/contentcatcher/ContentCatcherService$b;


# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .locals 18
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Landroid/os/ServiceManager$ServiceNotFoundException;
        }
    .end annotation

    move-object/from16 v1, p0

    move-object/from16 v2, p1

    const/4 v3, 0x1

    invoke-direct/range {p0 .. p0}, Lmiui/contentcatcher/IContentCatcherService$Stub;-><init>()V

    const/4 v4, 0x0

    iput-object v4, v1, Lcom/miui/contentcatcher/ContentCatcherService;->a:Lw/a;

    new-instance v0, Ljava/util/HashSet;

    invoke-direct {v0}, Ljava/util/HashSet;-><init>()V

    iput-object v0, v1, Lcom/miui/contentcatcher/ContentCatcherService;->e:Ljava/util/HashSet;

    new-instance v0, Ljava/util/HashSet;

    invoke-direct {v0}, Ljava/util/HashSet;-><init>()V

    iput-object v0, v1, Lcom/miui/contentcatcher/ContentCatcherService;->f:Ljava/util/HashSet;

    new-instance v0, Lcom/miui/contentcatcher/ContentCatcherService$a;

    invoke-direct {v0}, Landroid/content/IOnPrimaryClipChangedListener$Stub;-><init>()V

    iput-object v0, v1, Lcom/miui/contentcatcher/ContentCatcherService;->h:Lcom/miui/contentcatcher/ContentCatcherService$a;

    new-instance v0, Lcom/miui/contentcatcher/ContentCatcherService$c;

    invoke-direct {v0, v1}, Lcom/miui/contentcatcher/ContentCatcherService$c;-><init>(Lcom/miui/contentcatcher/ContentCatcherService;)V

    iput-object v0, v1, Lcom/miui/contentcatcher/ContentCatcherService;->i:Lcom/miui/contentcatcher/ContentCatcherService$c;

    new-instance v5, Lcom/miui/contentcatcher/ContentCatcherService$b;

    invoke-direct {v5, v1}, Lcom/miui/contentcatcher/ContentCatcherService$b;-><init>(Lcom/miui/contentcatcher/ContentCatcherService;)V

    iput-object v5, v1, Lcom/miui/contentcatcher/ContentCatcherService;->j:Lcom/miui/contentcatcher/ContentCatcherService$b;

    iput-object v2, v1, Lcom/miui/contentcatcher/ContentCatcherService;->b:Landroid/content/Context;

    sget-object v5, Lw/a;->f:Lw/a;

    if-nez v5, :cond_1

    const-class v5, Lw/a;

    monitor-enter v5

    :try_start_0
    sget-object v6, Lw/a;->f:Lw/a;

    if-nez v6, :cond_0

    new-instance v6, Lw/a;

    invoke-direct {v6, v2, v0}, Lw/a;-><init>(Landroid/content/Context;Lcom/miui/contentcatcher/ContentCatcherService$c;)V

    sput-object v6, Lw/a;->f:Lw/a;

    goto :goto_0

    :catchall_0
    move-exception v0

    goto :goto_1

    :cond_0
    :goto_0
    monitor-exit v5

    goto :goto_2

    :goto_1
    monitor-exit v5
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw v0

    :cond_1
    :goto_2
    sget-object v0, Lw/a;->f:Lw/a;

    iput-object v0, v1, Lcom/miui/contentcatcher/ContentCatcherService;->a:Lw/a;

    const-string v0, "clipboard"

    # getServiceOrThrow() is a hidden API that the platform-signed
    # ContentCatcher may use, but a re-signed patch loses that exemption and
    # crashes during ContentCatcherService.<init> on Android 17. getService()
    # is public and only returns null when the clipboard service is absent.
    invoke-static {v0}, Landroid/os/ServiceManager;->getService(Ljava/lang/String;)Landroid/os/IBinder;

    move-result-object v0

    invoke-static {v0}, Landroid/content/IClipboard$Stub;->asInterface(Landroid/os/IBinder;)Landroid/content/IClipboard;

    move-result-object v0

    iput-object v0, v1, Lcom/miui/contentcatcher/ContentCatcherService;->g:Landroid/content/IClipboard;

    invoke-static/range {p1 .. p1}, Lcom/miui/contentcatcher/service/config/a;->e(Landroid/content/Context;)Lcom/miui/contentcatcher/service/config/a;

    move-result-object v5

    const-string v6, "view"

    const-string v7, "config"

    iget-object v0, v5, Lcom/miui/contentcatcher/service/config/a;->a:Landroid/content/Context;

    :try_start_1
    invoke-virtual {v0}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;

    move-result-object v0

    invoke-virtual {v0, v7}, Landroid/content/res/AssetManager;->list(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v0

    new-instance v9, Ljava/util/ArrayList;

    invoke-direct {v9}, Ljava/util/ArrayList;-><init>()V

    array-length v10, v0

    const/4 v11, 0x0

    :goto_3
    if-ge v11, v10, :cond_3

    aget-object v12, v0, v11

    invoke-virtual {v12, v7}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v13

    if-eqz v13, :cond_2

    invoke-virtual {v12, v6}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v13

    if-nez v13, :cond_2

    invoke-virtual {v9, v12}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    :try_end_1
    .catch Ljava/io/IOException; {:try_start_1 .. :try_end_1} :catch_0

    goto :goto_4

    :catch_0
    move-exception v0

    goto :goto_5

    :cond_2
    :goto_4
    add-int/2addr v11, v3

    goto :goto_3

    :goto_5
    const-string v9, "FileSyncHelper"

    const-string v10, "IOException"

    invoke-static {v9, v10, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    move-object v9, v4

    :cond_3
    if-eqz v9, :cond_12

    invoke-interface {v9}, Ljava/util/List;->isEmpty()Z

    move-result v0

    if-nez v0, :cond_12

    iget-object v0, v5, Lcom/miui/contentcatcher/service/config/a;->c:Ljava/util/ArrayList;

    invoke-virtual {v0}, Ljava/util/ArrayList;->clear()V

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v10

    invoke-interface {v9}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v9

    :goto_6
    invoke-interface {v9}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    const-string v12, "ConfigHelper"

    if-eqz v0, :cond_11

    invoke-interface {v9}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    new-instance v13, Ljava/lang/StringBuilder;

    const-string v14, "loadAllConfigFromAssets = "

    invoke-direct {v13, v14}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v13, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v13}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v13

    invoke-static {v12, v13}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v13, v5, Lcom/miui/contentcatcher/service/config/a;->a:Landroid/content/Context;

    if-eqz v13, :cond_4

    invoke-virtual {v13}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;

    move-result-object v13

    goto :goto_7

    :cond_4
    move-object v13, v4

    :goto_7
    if-nez v13, :cond_5

    const-string v0, "load() assetManager is null"

    invoke-static {v12, v0}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    goto/16 :goto_12

    :cond_5
    new-instance v14, Ljava/util/ArrayList;

    invoke-direct {v14}, Ljava/util/ArrayList;-><init>()V

    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v15

    if-eqz v15, :cond_8

    :try_start_2
    invoke-virtual {v13, v7}, Landroid/content/res/AssetManager;->list(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v0

    array-length v15, v0

    const/4 v8, 0x0

    :goto_8
    if-ge v8, v15, :cond_7

    aget-object v4, v0, v8

    invoke-virtual {v4, v7}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v16

    if-eqz v16, :cond_6

    invoke-virtual {v4, v6}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v16

    if-nez v16, :cond_6

    invoke-virtual {v14, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_6
    add-int/2addr v8, v3

    const/4 v4, 0x0

    goto :goto_8

    :cond_7
    invoke-virtual {v14}, Ljava/util/ArrayList;->size()I

    move-result v0

    if-lez v0, :cond_9

    iget-object v0, v5, Lcom/miui/contentcatcher/service/config/a;->c:Ljava/util/ArrayList;

    invoke-virtual {v0}, Ljava/util/ArrayList;->clear()V
    :try_end_2
    .catch Ljava/io/IOException; {:try_start_2 .. :try_end_2} :catch_8

    goto :goto_9

    :cond_8
    invoke-virtual {v14, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_9
    :goto_9
    invoke-virtual {v14}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v4

    :goto_a
    invoke-interface {v4}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_10

    invoke-interface {v4}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    new-instance v8, Ljava/lang/StringBuilder;

    const-string v14, "file name "

    invoke-direct {v8, v14}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v8, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v8

    invoke-static {v12, v8}, Lp/b;->c(Ljava/lang/String;Ljava/lang/String;)V

    :try_start_3
    new-instance v8, Ljava/lang/StringBuilder;

    invoke-direct {v8}, Ljava/lang/StringBuilder;-><init>()V

    const-string v14, "config/"

    invoke-virtual {v8, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v13, v0}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;

    move-result-object v8
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_5
    .catchall {:try_start_3 .. :try_end_3} :catchall_4

    :try_start_4
    new-instance v14, Ljava/io/InputStreamReader;

    invoke-direct {v14, v8}, Ljava/io/InputStreamReader;-><init>(Ljava/io/InputStream;)V
    :try_end_4
    .catch Ljava/lang/Exception; {:try_start_4 .. :try_end_4} :catch_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_3

    :try_start_5
    new-instance v15, Landroid/util/JsonReader;

    invoke-direct {v15, v14}, Landroid/util/JsonReader;-><init>(Ljava/io/Reader;)V
    :try_end_5
    .catch Ljava/lang/Exception; {:try_start_5 .. :try_end_5} :catch_3
    .catchall {:try_start_5 .. :try_end_5} :catchall_2

    :try_start_6
    invoke-virtual {v5, v15}, Lcom/miui/contentcatcher/service/config/a;->i(Landroid/util/JsonReader;)V
    :try_end_6
    .catch Ljava/lang/Exception; {:try_start_6 .. :try_end_6} :catch_2
    .catchall {:try_start_6 .. :try_end_6} :catchall_1

    :try_start_7
    invoke-virtual {v15}, Landroid/util/JsonReader;->close()V
    :try_end_7
    .catch Ljava/io/IOException; {:try_start_7 .. :try_end_7} :catch_1

    :catch_1
    move-object/from16 v17, v4

    goto :goto_f

    :catchall_1
    move-exception v0

    move-object v4, v15

    goto :goto_10

    :catch_2
    move-exception v0

    goto :goto_e

    :catchall_2
    move-exception v0

    const/4 v4, 0x0

    goto :goto_10

    :catch_3
    move-exception v0

    :goto_b
    const/4 v15, 0x0

    goto :goto_e

    :catchall_3
    move-exception v0

    const/4 v4, 0x0

    :goto_c
    const/4 v14, 0x0

    goto :goto_10

    :catch_4
    move-exception v0

    :goto_d
    const/4 v14, 0x0

    goto :goto_b

    :catchall_4
    move-exception v0

    const/4 v4, 0x0

    const/4 v8, 0x0

    goto :goto_c

    :catch_5
    move-exception v0

    const/4 v8, 0x0

    goto :goto_d

    :goto_e
    :try_start_8
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    move-object/from16 v17, v4

    const-string v4, " loadConfigFromAssets  e :"

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v12, v0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_8
    .catchall {:try_start_8 .. :try_end_8} :catchall_1

    if-eqz v15, :cond_a

    :try_start_9
    invoke-virtual {v15}, Landroid/util/JsonReader;->close()V

    goto :goto_f

    :cond_a
    if-eqz v14, :cond_b

    invoke-virtual {v14}, Ljava/io/InputStreamReader;->close()V

    goto :goto_f

    :cond_b
    if-eqz v8, :cond_c

    invoke-virtual {v8}, Ljava/io/InputStream;->close()V
    :try_end_9
    .catch Ljava/io/IOException; {:try_start_9 .. :try_end_9} :catch_6

    :catch_6
    :cond_c
    :goto_f
    move-object/from16 v4, v17

    const/4 v3, 0x1

    goto/16 :goto_a

    :goto_10
    if-nez v4, :cond_e

    if-nez v14, :cond_d

    if-eqz v8, :cond_f

    :try_start_a
    invoke-virtual {v8}, Ljava/io/InputStream;->close()V

    goto :goto_11

    :cond_d
    invoke-virtual {v14}, Ljava/io/InputStreamReader;->close()V

    goto :goto_11

    :cond_e
    invoke-virtual {v4}, Landroid/util/JsonReader;->close()V
    :try_end_a
    .catch Ljava/io/IOException; {:try_start_a .. :try_end_a} :catch_7

    :catch_7
    :cond_f
    :goto_11
    throw v0

    :catch_8
    :cond_10
    :goto_12
    const/4 v3, 0x1

    const/4 v4, 0x0

    goto/16 :goto_6

    :cond_11
    new-instance v0, Ljava/lang/StringBuilder;

    const-string v3, "load all config took "

    invoke-direct {v0, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v3

    sub-long/2addr v3, v10

    invoke-virtual {v0, v3, v4}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    const-string v3, " ms"

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v12, v0}, Lp/b;->i(Ljava/lang/String;Ljava/lang/String;)V

    :cond_12
    invoke-static/range {p1 .. p1}, Lcom/miui/contentcatcher/service/config/a;->e(Landroid/content/Context;)Lcom/miui/contentcatcher/service/config/a;

    move-result-object v0

    invoke-virtual {v0}, Lcom/miui/contentcatcher/service/config/a;->h()V

    invoke-virtual/range {p1 .. p1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v0

    const-string v3, "support_double_press_mode"

    const/4 v4, 0x1

    invoke-static {v0, v3, v4}, Landroid/provider/Settings$System;->putInt(Landroid/content/ContentResolver;Ljava/lang/String;I)Z

    invoke-static {}, Landroid/os/UserHandle;->myUserId()I

    move-result v0

    iput v0, v1, Lcom/miui/contentcatcher/ContentCatcherService;->d:I

    iget-object v0, v1, Lcom/miui/contentcatcher/ContentCatcherService;->b:Landroid/content/Context;

    const-string v3, "clipboard"

    invoke-virtual {v0, v3}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/content/ClipboardManager;

    iput-object v0, v1, Lcom/miui/contentcatcher/ContentCatcherService;->c:Landroid/content/ClipboardManager;

    :try_start_b
    const-class v0, Landroid/provider/Settings$System;

    const-string v3, "putIntForUser"

    const-class v4, Landroid/content/ContentResolver;

    const-class v5, Ljava/lang/String;

    sget-object v6, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    filled-new-array {v4, v5, v6, v6}, [Ljava/lang/Class;

    move-result-object v4

    iget-object v5, v1, Lcom/miui/contentcatcher/ContentCatcherService;->b:Landroid/content/Context;

    invoke-virtual {v5}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v5

    const-string v6, "support_clipboard_mode"

    const/4 v7, 0x1

    invoke-static {v7}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v8

    iget v7, v1, Lcom/miui/contentcatcher/ContentCatcherService;->d:I

    invoke-static {v7}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v7

    filled-new-array {v5, v6, v8, v7}, [Ljava/lang/Object;

    move-result-object v5

    sget-object v6, Lp/c;->a:Ljava/util/HashMap;

    invoke-virtual {v0, v3, v4}, Ljava/lang/Class;->getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v0

    const/4 v3, 0x1

    invoke-virtual {v0, v3}, Ljava/lang/reflect/AccessibleObject;->setAccessible(Z)V

    const/4 v3, 0x0

    invoke-virtual {v0, v3, v5}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_b
    .catch Ljava/lang/Exception; {:try_start_b .. :try_end_b} :catch_9

    goto :goto_13

    :catch_9
    move-exception v0

    const-string v3, "ContentCatcherService"

    const-string v4, "ContentCatcherService putIntForUser error: "

    invoke-static {v4, v0, v3}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :goto_13
    new-instance v0, Landroid/content/IntentFilter;

    invoke-direct {v0}, Landroid/content/IntentFilter;-><init>()V

    const-string v3, "android.intent.action.USER_SWITCHED"

    invoke-virtual {v0, v3}, Landroid/content/IntentFilter;->addAction(Ljava/lang/String;)V

    :try_start_c
    const-string v3, "registerReceiverForAllUsers"

    const-class v4, Landroid/content/BroadcastReceiver;

    const-class v5, Landroid/content/IntentFilter;

    const-class v6, Ljava/lang/String;

    const-class v7, Landroid/os/Handler;

    sget-object v8, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    filled-new-array {v4, v5, v6, v7, v8}, [Ljava/lang/Class;

    move-result-object v4

    iget-object v5, v1, Lcom/miui/contentcatcher/ContentCatcherService;->j:Lcom/miui/contentcatcher/ContentCatcherService$b;

    const/4 v6, 0x2

    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v6

    const/4 v7, 0x0

    filled-new-array {v5, v0, v7, v7, v6}, [Ljava/lang/Object;

    move-result-object v0

    invoke-static {v2, v3, v4, v0}, Lp/c;->b(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_c
    .catch Ljava/lang/Exception; {:try_start_c .. :try_end_c} :catch_a

    goto :goto_14

    :catch_a
    move-exception v0

    const-string v2, "registerUserSwitchReceiver error: "

    const-string v3, "ContentCatcherService"

    invoke-static {v2, v0, v3}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :goto_14
    new-instance v0, Lcom/miui/contentcatcher/ContentCatcherService$d;

    iget-object v2, v1, Lcom/miui/contentcatcher/ContentCatcherService;->i:Lcom/miui/contentcatcher/ContentCatcherService$c;

    invoke-direct {v0, v1, v2}, Lcom/miui/contentcatcher/ContentCatcherService$d;-><init>(Lcom/miui/contentcatcher/ContentCatcherService;Landroid/os/Handler;)V

    iget-object v2, v1, Lcom/miui/contentcatcher/ContentCatcherService;->b:Landroid/content/Context;

    invoke-virtual {v2}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v2

    :try_start_d
    const-string v3, "registerContentObserver"

    const-class v4, Landroid/net/Uri;

    sget-object v5, Ljava/lang/Boolean;->TYPE:Ljava/lang/Class;

    const-class v6, Landroid/database/ContentObserver;

    sget-object v7, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    filled-new-array {v4, v5, v6, v7}, [Ljava/lang/Class;

    move-result-object v4

    const-string v5, "taplus_clipboard_mode"

    invoke-static {v5}, Landroid/provider/Settings$System;->getUriFor(Ljava/lang/String;)Landroid/net/Uri;

    move-result-object v5

    sget-object v6, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;

    const/4 v7, -0x1

    invoke-static {v7}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v7

    filled-new-array {v5, v6, v0, v7}, [Ljava/lang/Object;

    move-result-object v0

    invoke-static {v2, v3, v4, v0}, Lp/c;->b(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_d
    .catch Ljava/lang/Exception; {:try_start_d .. :try_end_d} :catch_b

    goto :goto_15

    :catch_b
    move-exception v0

    const-string v2, "registerContentObserver error : "

    const-string v3, "ContentCatcherService"

    invoke-static {v2, v0, v3}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :goto_15
    iget v0, v1, Lcom/miui/contentcatcher/ContentCatcherService;->d:I

    iput v0, v1, Lcom/miui/contentcatcher/ContentCatcherService;->d:I

    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v0

    iget-object v2, v1, Lcom/miui/contentcatcher/ContentCatcherService;->e:Ljava/util/HashSet;

    invoke-virtual {v2, v0}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    invoke-virtual/range {p0 .. p0}, Lcom/miui/contentcatcher/ContentCatcherService;->a()V

    return-void
.end method


# virtual methods
.method public final a()V
    .locals 12

    iget-object v0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->b:Landroid/content/Context;

    const-class v1, Ljava/lang/String;

    const-string v2, "ContentCatcherService"

    iget-object v3, p0, Lcom/miui/contentcatcher/ContentCatcherService;->c:Landroid/content/ClipboardManager;

    if-nez v3, :cond_0

    return-void

    :cond_0
    const/4 v3, 0x1

    :try_start_0
    const-class v4, Landroid/provider/Settings$System;

    const-string v5, "getIntForUser"

    const-class v6, Landroid/content/ContentResolver;

    sget-object v7, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    filled-new-array {v6, v1, v7, v7}, [Ljava/lang/Class;

    move-result-object v6

    invoke-virtual {v0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v7

    const-string v8, "taplus_clipboard_mode"

    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v9

    iget v10, p0, Lcom/miui/contentcatcher/ContentCatcherService;->d:I

    invoke-static {v10}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v10

    filled-new-array {v7, v8, v9, v10}, [Ljava/lang/Object;

    move-result-object v7

    sget-object v8, Lp/c;->a:Ljava/util/HashMap;

    invoke-virtual {v4, v5, v6}, Ljava/lang/Class;->getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v4

    invoke-virtual {v4, v3}, Ljava/lang/reflect/AccessibleObject;->setAccessible(Z)V

    const/4 v5, 0x0

    invoke-virtual {v4, v5, v7}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/Integer;

    invoke-virtual {v4}, Ljava/lang/Integer;->intValue()I

    move-result v4
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception v4

    const-string v5, "getIntForUser KEY_TAPLUS_CLIPBOARD_MODE error: "

    invoke-static {v5, v4, v2}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    move v4, v3

    :goto_0
    iget-object v5, p0, Lcom/miui/contentcatcher/ContentCatcherService;->f:Ljava/util/HashSet;

    const/4 v6, 0x0

    iget-object v7, p0, Lcom/miui/contentcatcher/ContentCatcherService;->h:Lcom/miui/contentcatcher/ContentCatcherService$a;

    const-class v8, Landroid/content/IOnPrimaryClipChangedListener;

    iget-object v9, p0, Lcom/miui/contentcatcher/ContentCatcherService;->g:Landroid/content/IClipboard;

    if-ne v4, v3, :cond_2

    const-string v3, "open function,startingWatching clipboard"

    invoke-static {v2, v3}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->d:I

    invoke-static {p0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v3

    invoke-virtual {v5, v3}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_1

    :try_start_1
    invoke-virtual {v9}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v3

    const-string v4, "addPrimaryClipChangedListener"

    sget-object v10, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    filled-new-array {v8, v1, v1, v10, v10}, [Ljava/lang/Class;

    move-result-object v1

    invoke-virtual {v3, v4, v1}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v1

    invoke-virtual {v0}, Landroid/content/Context;->getOpPackageName()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v0}, Landroid/content/Context;->getAttributionTag()Ljava/lang/String;

    move-result-object v0

    invoke-static {p0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v6

    filled-new-array {v7, v3, v0, v4, v6}, [Ljava/lang/Object;

    move-result-object v0

    invoke-virtual {v1, v9, v0}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1

    goto :goto_1

    :catch_1
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v1

    invoke-static {v2, v1, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    :cond_1
    :goto_1
    invoke-static {p0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p0

    invoke-virtual {v5, p0}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    goto :goto_3

    :cond_2
    const-string v3, "close function, unStartingWatching clipboard"

    invoke-static {v2, v3}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    iget v3, p0, Lcom/miui/contentcatcher/ContentCatcherService;->d:I

    invoke-virtual {v5}, Ljava/util/HashSet;->isEmpty()Z

    move-result v4

    if-nez v4, :cond_3

    :try_start_2
    invoke-virtual {v9}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v4

    const-string v10, "removePrimaryClipChangedListener"

    sget-object v11, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    filled-new-array {v8, v1, v1, v11, v11}, [Ljava/lang/Class;

    move-result-object v1

    invoke-virtual {v4, v10, v1}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v1

    invoke-virtual {v0}, Landroid/content/Context;->getOpPackageName()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v0}, Landroid/content/Context;->getAttributionTag()Ljava/lang/String;

    move-result-object v0

    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v8

    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v6

    filled-new-array {v7, v4, v0, v8, v6}, [Ljava/lang/Object;

    move-result-object v0

    invoke-virtual {v1, v9, v0}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_2

    goto :goto_2

    :catch_2
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v1

    invoke-static {v2, v1, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    :cond_3
    :goto_2
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v0

    invoke-virtual {v5, v0}, Ljava/util/HashSet;->remove(Ljava/lang/Object;)Z

    iget v0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->d:I

    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v0

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->e:Ljava/util/HashSet;

    invoke-virtual {p0, v0}, Ljava/util/HashSet;->remove(Ljava/lang/Object;)Z

    :goto_3
    return-void
.end method

.method public final decorateContent(Lmiui/contentcatcher/sdk/ClientToken;Lmiui/contentcatcher/sdk/Token;Lmiui/contentcatcher/sdk/DecorateContentParam;)V
    .locals 2

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "decorateContent listener "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string p1, ", target "

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string p1, ", params "

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p3}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {p1}, Lp/b;->h(Ljava/lang/String;)V

    if-nez p3, :cond_0

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->a:Lw/a;

    invoke-virtual {p0}, Lw/a;->b()V

    goto :goto_1

    :cond_0
    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->a:Lw/a;

    const-string p1, "decorateContent failed: "

    monitor-enter p0

    :try_start_0
    iget-object v0, p0, Lw/a;->b:Lw/b;

    invoke-virtual {v0, p2}, Lw/b;->b(Ljava/lang/Object;)Landroid/os/IInterface;

    move-result-object p2

    check-cast p2, Lmiui/contentcatcher/sdk/injector/IContentDecorateCallback;
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eqz p2, :cond_1

    :try_start_1
    invoke-interface {p2, p3}, Lmiui/contentcatcher/sdk/injector/IContentDecorateCallback;->updateContent(Lmiui/contentcatcher/sdk/DecorateContentParam;)V
    :try_end_1
    .catch Landroid/os/RemoteException; {:try_start_1 .. :try_end_1} :catch_0
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    goto :goto_0

    :catchall_0
    move-exception p1

    goto :goto_2

    :catch_0
    move-exception p2

    :try_start_2
    const-string p3, "DataHub"

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0, p1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p2}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {p3, p1}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    :cond_1
    :goto_0
    monitor-exit p0

    :goto_1
    return-void

    :goto_2
    monitor-exit p0
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    throw p1
.end method

.method public final getPageConfig(Lmiui/contentcatcher/sdk/Token;)Lmiui/contentcatcher/sdk/data/PageConfig;
    .locals 8

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "getPageConfig of "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Lp/b;->h(Ljava/lang/String;)V

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->b:Landroid/content/Context;

    invoke-static {p0}, Lcom/miui/contentcatcher/service/config/a;->e(Landroid/content/Context;)Lcom/miui/contentcatcher/service/config/a;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/Token;->getPkgName()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/Token;->getActivityName()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/Token;->getVersionCode()I

    move-result v2

    const-string v3, "ConfigHelper"

    const-string v4, "getPageConfig features: "

    const/4 v5, 0x0

    :try_start_0
    new-instance v6, Ljava/util/ArrayList;

    invoke-direct {v6}, Ljava/util/ArrayList;-><init>()V

    invoke-virtual {p0, v0, v1, v2, v6}, Lcom/miui/contentcatcher/service/config/a;->d(Ljava/lang/String;Ljava/lang/String;ILjava/util/ArrayList;)V

    new-instance v7, Ljava/util/ArrayList;

    invoke-direct {v7}, Ljava/util/ArrayList;-><init>()V

    invoke-virtual {p0, v0, v1, v2, v7}, Lcom/miui/contentcatcher/service/config/a;->c(Ljava/lang/String;Ljava/lang/String;ILjava/util/ArrayList;)V

    new-instance p0, Ljava/lang/StringBuilder;

    invoke-direct {p0, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v0, ", catchers:"

    invoke-virtual {p0, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v3, p0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    new-instance p0, Lmiui/contentcatcher/sdk/data/PageConfig;

    invoke-direct {p0, v6, v7}, Lmiui/contentcatcher/sdk/data/PageConfig;-><init>(Ljava/util/ArrayList;Ljava/util/ArrayList;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p0

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, " getPageConfig  e :"

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v3, p0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    move-object p0, v5

    :goto_0
    if-eqz p0, :cond_1

    invoke-virtual {p0}, Lmiui/contentcatcher/sdk/data/PageConfig;->getCatchers()Ljava/util/ArrayList;

    move-result-object v0

    if-nez v0, :cond_0

    invoke-virtual {p0}, Lmiui/contentcatcher/sdk/data/PageConfig;->getFeatures()Ljava/util/ArrayList;

    move-result-object v0

    if-nez v0, :cond_0

    goto :goto_1

    :cond_0
    return-object p0

    :cond_1
    :goto_1
    new-instance p0, Ljava/lang/StringBuilder;

    const-string v0, "None Config of "

    invoke-direct {p0, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {p0}, Lp/b;->h(Ljava/lang/String;)V

    return-object v5
.end method

.method public final onContentCatched(Lmiui/contentcatcher/sdk/Content;)V
    .locals 2

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "onContentCatched "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Lp/b;->h(Ljava/lang/String;)V

    invoke-static {}, Landroid/os/Message;->obtain()Landroid/os/Message;

    move-result-object v0

    const/16 v1, 0x64

    iput v1, v0, Landroid/os/Message;->what:I

    iput-object p1, v0, Landroid/os/Message;->obj:Ljava/lang/Object;

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->i:Lcom/miui/contentcatcher/ContentCatcherService$c;

    invoke-virtual {p0, v0}, Landroid/os/Handler;->sendMessage(Landroid/os/Message;)Z

    return-void
.end method

.method public final registerContentInjector(Lmiui/contentcatcher/sdk/Token;Lmiui/contentcatcher/sdk/injector/IContentDecorateCallback;)V
    .locals 2

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "registerContentInjector "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v1, ", callback "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Lp/b;->h(Ljava/lang/String;)V

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->a:Lw/a;

    const-string v0, "register failed: already registered "

    monitor-enter p0

    if-eqz p2, :cond_1

    :try_start_0
    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/Token;->toString()Ljava/lang/String;

    move-result-object p1

    iget-object v1, p0, Lw/a;->b:Lw/b;

    invoke-virtual {v1, p1}, Lw/b;->a(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_0

    const-string p1, "DataHub"

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-static {p1, p2}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    monitor-exit p0

    goto :goto_0

    :catchall_0
    move-exception p1

    goto :goto_1

    :cond_0
    iget-object v0, p0, Lw/a;->b:Lw/b;

    invoke-virtual {v0, p1, p2}, Lw/b;->f(Ljava/lang/Object;Landroid/os/IInterface;)V

    :cond_1
    monitor-exit p0

    :goto_0
    return-void

    :goto_1
    monitor-exit p0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw p1
.end method

.method public final registerContentListener(Lmiui/contentcatcher/sdk/ClientToken;Lmiui/contentcatcher/sdk/listener/IContentListenerCallback;)V
    .locals 4

    const-string v0, "registerContentListener"

    invoke-static {v0}, Lx/a;->b(Ljava/lang/String;)V

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "registerContentListener "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v1, ", callback "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Lp/b;->h(Ljava/lang/String;)V

    iget-object v0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->a:Lw/a;

    const-string v1, "register listener failed: already registered: "

    monitor-enter v0

    :try_start_0
    iget-object v2, v0, Lw/a;->a:Lw/b;

    invoke-virtual {v2, p1}, Lw/b;->a(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_0

    const-string v2, "DataHub"

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v2, v1}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_0

    :catchall_0
    move-exception p0

    goto :goto_1

    :cond_0
    :goto_0
    iget-object v1, v0, Lw/a;->a:Lw/b;

    invoke-virtual {v1, p1, p2}, Lw/b;->f(Ljava/lang/Object;Landroid/os/IInterface;)V

    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    invoke-static {}, Landroid/os/Message;->obtain()Landroid/os/Message;

    move-result-object p2

    const/16 v0, 0x66

    iput v0, p2, Landroid/os/Message;->what:I

    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/ClientToken;->getPkgName()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p2, Landroid/os/Message;->obj:Ljava/lang/Object;

    iget-object v0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->i:Lcom/miui/contentcatcher/ContentCatcherService$c;

    invoke-virtual {v0, p2}, Landroid/os/Handler;->sendMessage(Landroid/os/Message;)Z

    const-string p2, "com.miui.voiceassistant"

    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/ClientToken;->getPkgName()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {p2, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_1

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->b:Landroid/content/Context;

    invoke-virtual {p0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object p0

    const-string p1, "com.miui.voiceassist.preferences.screenhelper"

    const/4 p2, 0x1

    invoke-static {p0, p1, p2}, Landroid/provider/Settings$Global;->putInt(Landroid/content/ContentResolver;Ljava/lang/String;I)Z

    :cond_1
    return-void

    :goto_1
    :try_start_1
    monitor-exit v0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    throw p0
.end method

.method public final unregisterContentInjector(Lmiui/contentcatcher/sdk/Token;)V
    .locals 3

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "unregisterContentInjector "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Lp/b;->h(Ljava/lang/String;)V

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->a:Lw/a;

    const-string v0, "unregister failed: not registered "

    monitor-enter p0

    if-eqz p1, :cond_1

    :try_start_0
    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/Token;->toString()Ljava/lang/String;

    move-result-object v1

    iget-object v2, p0, Lw/a;->b:Lw/b;

    invoke-virtual {v2, v1}, Lw/b;->a(Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_0

    const-string v1, "DataHub"

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v1, p1}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    monitor-exit p0

    goto :goto_0

    :catchall_0
    move-exception p1

    goto :goto_1

    :cond_0
    iget-object p1, p0, Lw/a;->b:Lw/b;

    invoke-virtual {p1, v1}, Lw/b;->g(Ljava/lang/Object;)V

    :cond_1
    monitor-exit p0

    :goto_0
    return-void

    :goto_1
    monitor-exit p0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw p1
.end method

.method public final unregisterContentListener(Lmiui/contentcatcher/sdk/ClientToken;)V
    .locals 4

    const-string v0, "unregisterContentListener"

    invoke-static {v0}, Lx/a;->b(Ljava/lang/String;)V

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "unregisterContentListener "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Lp/b;->h(Ljava/lang/String;)V

    iget-object v0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->a:Lw/a;

    const-string v1, "unregister listener failed: not registered: "

    monitor-enter v0

    if-eqz p1, :cond_1

    :try_start_0
    iget-object v2, v0, Lw/a;->a:Lw/b;

    invoke-virtual {v2, p1}, Lw/b;->a(Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_0

    const-string v2, "DataHub"

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v2, v1}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_0

    :catchall_0
    move-exception p0

    goto :goto_1

    :cond_0
    :goto_0
    iget-object v1, v0, Lw/a;->a:Lw/b;

    invoke-virtual {v1, p1}, Lw/b;->g(Ljava/lang/Object;)V

    :cond_1
    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    const-string v0, "com.miui.voiceassistant"

    invoke-virtual {p1}, Lmiui/contentcatcher/sdk/ClientToken;->getPkgName()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_2

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->b:Landroid/content/Context;

    invoke-virtual {p0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object p0

    const-string p1, "com.miui.voiceassist.preferences.screenhelper"

    const/4 v0, 0x0

    invoke-static {p0, p1, v0}, Landroid/provider/Settings$Global;->putInt(Landroid/content/ContentResolver;Ljava/lang/String;I)Z

    :cond_2
    return-void

    :goto_1
    :try_start_1
    monitor-exit v0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    throw p0
.end method

.method public final updateClientConfig(Ljava/lang/String;Ljava/lang/String;Z)V
    .locals 9

    const-string v0, "updateClientConfig"

    invoke-static {v0}, Lx/a;->b(Ljava/lang/String;)V

    iget-object v0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->b:Landroid/content/Context;

    invoke-static {v0}, Lcom/miui/contentcatcher/service/config/a;->e(Landroid/content/Context;)Lcom/miui/contentcatcher/service/config/a;

    move-result-object v0

    iget-object v0, v0, Lcom/miui/contentcatcher/service/config/a;->c:Ljava/util/ArrayList;

    invoke-virtual {v0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v0

    const/4 v1, 0x0

    :cond_0
    :goto_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_2

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lcom/miui/contentcatcher/service/config/a$c;

    invoke-static {p1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v3

    if-nez v3, :cond_0

    iget-object v3, v2, Lcom/miui/contentcatcher/service/config/a$c;->b:Ljava/lang/String;

    invoke-virtual {p1, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_0

    invoke-static {p2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v3

    if-nez v3, :cond_1

    iget-object v3, v2, Lcom/miui/contentcatcher/service/config/a$c;->c:Ljava/lang/String;

    invoke-virtual {p2, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_0

    :cond_1
    invoke-static {v2, p3}, Lcom/miui/contentcatcher/service/config/a;->m(Lcom/miui/contentcatcher/service/config/a$c;Z)Z

    move-result v2

    if-eqz v2, :cond_0

    const/4 v1, 0x1

    goto :goto_0

    :cond_2
    new-instance v0, Ljava/lang/StringBuilder;

    const-string v2, "updateClientConfig target="

    invoke-direct {v0, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v2, ", jobTag="

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v2, ", enable="

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p3}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    const-string v2, ", updated="

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Lp/b;->h(Ljava/lang/String;)V

    if-eqz v1, :cond_4

    iget-object v2, p0, Lcom/miui/contentcatcher/ContentCatcherService;->a:Lw/a;

    invoke-virtual {v2}, Lw/a;->b()V

    if-eqz p3, :cond_3

    const-string p0, "1"

    :goto_1
    move-object v5, p0

    goto :goto_2

    :cond_3
    const-string p0, "0"

    goto :goto_1

    :goto_2
    const/4 v6, 0x0

    const/4 v7, 0x0

    const/4 v8, 0x0

    move-object v3, p1

    move-object v4, p2

    invoke-virtual/range {v2 .. v8}, Lw/a;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lmiui/contentcatcher/sdk/Token;)V

    :cond_4
    return-void
.end method

.method public final updateConfig([Ljava/lang/String;)V
    .locals 0

    const-string p0, "updateConfig"

    invoke-static {p0}, Lx/a;->b(Ljava/lang/String;)V

    return-void
.end method

.method public final updateJobValidity(Ljava/lang/String;Ljava/lang/String;Z)V
    .locals 0

    const-string p0, "updateJobValidity"

    invoke-static {p0}, Lx/a;->b(Ljava/lang/String;)V

    return-void
.end method
