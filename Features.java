package ru.visuals;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.ArrayList;
import java.util.List;

public final class Features {
    private Features() { }

    public static final List<Feature> ALL = new ArrayList<>();
    public static final String[] CATS = {"Основное", "Визуалы", "Фигура", "Игра", "Интерфейс", "Тема"};

    public static final String[] PARTICLE_NAMES = {
            "Звёзды", "Магия", "Искры", "Руны", "Снежинки", "Души", "Огонь",
            "Лепестки", "Блёстки", "Пепел", "Свечение", "Сердца", "Ноты"
    };
    public static final ParticleEffect[] PARTICLES = {
            ParticleTypes.END_ROD, ParticleTypes.WITCH, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.ENCHANT,
            ParticleTypes.SNOWFLAKE, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.FLAME,
            ParticleTypes.CHERRY_LEAVES, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.WHITE_ASH,
            ParticleTypes.GLOW, ParticleTypes.HEART, ParticleTypes.NOTE
    };
    private static final String[] BG_STYLES = {"Стекло", "Тёмный", "Без фона"};
    private static final String[] TARGET_FILTER = {"Все", "Игроки", "Мобы"};

    private static Feature feat(String id, String name, String cat, boolean toggle, boolean def) {
        Feature f = new Feature(id, name, cat, toggle, def);
        ALL.add(f);
        return f;
    }

    // ===================== ОСНОВНОЕ =====================
    public static final Feature MENU = feat("menu", "Меню", "Основное", false, false);
    public static final Setting.Info M_KEY = MENU.info(() -> "Кнопка меню: " + VisualsClient.menuKeyName());
    public static final Setting.Info M_HINT = MENU.info(() -> "Назначить: .bind add в чате");
    public static final Setting.Bool M_GREET = MENU.bool("greet", "Приветствие в чате", true);
    public static final Setting.Action M_CLEAR = MENU.action("Убрать кнопку меню", () -> {
        Config.menuKey = -1;
        Config.save();
    });

    public static final Feature CONF = feat("config", "Конфиг", "Основное", false, false);
    public static final Setting.Action C_SAVE = CONF.action("Сохранить настройки", Config::save);
    public static final Setting.Action C_RESET = CONF.action("Сбросить всё", () -> {
        resetAll();
        Config.save();
    });

    public static final Feature ABOUT = feat("about", "О моде", "Основное", false, false);
    public static final Setting.Info A_VER = ABOUT.info(() -> "КК Visuals 1.5.0");
    public static final Setting.Info A_MC = ABOUT.info(() -> "Minecraft 1.21.4 · Fabric");

    // ===================== ВИЗУАЛЫ =====================
    public static final Feature FULLBRIGHT = feat("fullbright", "Fullbright", "Визуалы", true, false);
    public static final Setting.Num FB_LEVEL = FULLBRIGHT.num("level", "Яркость", 1, 16, 1, 16, "");

    public static final Feature ZOOM = feat("zoom", "Zoom", "Визуалы", true, true);
    public static final Setting.Num Z_FOV = ZOOM.num("fov", "Угол обзора", 5, 60, 1, 30, "°");
    public static final Setting.Num Z_SMOOTH = ZOOM.num("smooth", "Плавность", 0, 95, 5, 60, "%");
    public static final Setting.Info Z_HINT = ZOOM.info(() -> "Удерживать C (меняется в управлении)");

    public static final Feature TARGET = feat("target", "Target ESP", "Визуалы", true, true);
    public static final Setting.Mode TG_STYLE = TARGET.mode("style", "Частицы", 0, PARTICLE_NAMES);
    public static final Setting.Mode TG_MODE = TARGET.mode("mode", "Позиция", 0, "Ноги", "Тело", "Голова", "Сканер");
    public static final Setting.Num TG_RADIUS = TARGET.num("radius", "Радиус", 0.3, 2.5, 0.1, 1.0, "");
    public static final Setting.Num TG_COUNT = TARGET.num("count", "Количество", 4, 48, 2, 14, "");
    public static final Setting.Num TG_SPEED = TARGET.num("speed", "Скорость вращения", 0.02, 0.6, 0.02, 0.18, "");
    public static final Setting.Num TG_LIFT = TARGET.num("lift", "Подъём частиц", 0, 0.1, 0.005, 0.02, "");
    public static final Setting.Bool TG_SECOND = TARGET.bool("second", "Второе кольцо", false);
    public static final Setting.Num TG_TIME = TARGET.num("time", "Время показа", 1, 20, 1, 5, " с");
    public static final Setting.Mode TG_FILTER = TARGET.mode("filter", "Цели", 0, TARGET_FILTER);

    public static final Setting.Bool TG_AIM = TARGET.bool("aim", "Показывать при наведении", true);


    public static final Feature DEATH = feat("death", "Death Animation", "Визуалы", true, false);
    public static final Setting.Bool DA_67 = DEATH.bool("67", "Показывать 67 при убийстве", true);
    public static final Setting.Bool DA_TUNG = DEATH.bool("tung", "Тунг-тунг Сахур с неба", true);
    public static final Setting.Bool DA_MUSIC = DEATH.bool("music", "Музыка при убийстве", true);
    public static final Setting.Bool DA_SHAKER = DEATH.bool("shaker", "Тряска экрана", true);
    public static final Setting.Num DA_DURATION = DEATH.num("dur", "Длительность эффекта", 1, 8, 0.5, 3, " с");
    public static final Setting.Mode DA_MODE = DEATH.mode("mode", "Вид 67", 0, "Текст", "Большой текст", "Частицы 6 и 7");

    public static final Feature BADGE = feat("badge", "KK значок", "Визуалы", true, true);
    public static final Setting.Bool BD_SELF = BADGE.bool("self", "Над своим ником", false);
    public static final Setting.Mode BD_STYLE = BADGE.mode("style", "Стиль", 0, "KK", "✦KK✦", "《KK》", "✮KK✮");
    public static final Setting.Color BD_COLOR = BADGE.color("color", "Цвет", 1);
    public static final Setting.Bool BD_RAINBOW = BADGE.bool("rainbow", "Радуга", true);
    public static final Setting.Num BD_SCALE = BADGE.num("scale", "Размер", 0.5, 2, 0.1, 1, "x");
    public static final Setting.Num BD_Y = BADGE.num("y", "Высота над ником", 0, 1.5, 0.1, 0.5, "");

    public static final Feature MPLAYER = feat("mplayer", "Плеер музыки", "Интерфейс", true, false);
    public static final Setting.Bool MP_SHUFFLE = MPLAYER.bool("shuffle", "Случайный порядок", false);
    public static final Setting.Bool MP_LOOP = MPLAYER.bool("loop", "Повтор", true);
    public static final Setting.Num MP_VOL = MPLAYER.num("vol", "Громкость", 0, 100, 5, 70, "%");
    public static final Setting.Mode MP_POS = MPLAYER.mode("pos", "Позиция плеера", 0, "Лево верх", "Право верх", "Лево низ");
    public static final Setting.Bool MP_MINIFY = MPLAYER.bool("minify", "Мини-режим", false);
    public static final Setting.Info MP_HINT = MPLAYER.info(() -> "Нажмите CTRL+P чтобы открыть плейлист");
    public static final Setting.Action MP_OPEN = MPLAYER.action("Открыть файловый менеджер", () -> MusicPlayer.openFileChooser());

    public static final Feature HIT = feat("hit", "Hit Bubble", "Визуалы", true, true);
    public static final Setting.Mode H_STYLE = HIT.mode("style", "Частицы", 1, PARTICLE_NAMES);
    public static final Setting.Mode H_SHAPE = HIT.mode("shape", "Форма", 0, "Сфера", "Кольцо", "Фонтан", "Спираль");
    public static final Setting.Num H_COUNT = HIT.num("count", "Количество", 10, 150, 5, 50, "");
    public static final Setting.Num H_RADIUS = HIT.num("radius", "Радиус", 0.2, 1.5, 0.1, 0.4, "");
    public static final Setting.Num H_SPEED = HIT.num("speed", "Скорость", 0.02, 0.5, 0.02, 0.12, "");
    public static final Setting.Bool H_DEATH = HIT.bool("death", "Взрыв при смерти цели", true);
    public static final Setting.Num H_DEATH_MULT = HIT.num("deathmult", "Сила взрыва", 1, 5, 1, 2, "x");
    public static final Setting.Mode H_FILTER = HIT.mode("filter", "Цели", 0, TARGET_FILTER);

    public static final Feature PART = feat("particles", "Частицы", "Визуалы", true, true);
    public static final Setting.Mode P_STYLE = PART.mode("style", "Стиль", 0, PARTICLE_NAMES);
    public static final Setting.Mode P_MOVE = PART.mode("move", "Движение", 0, "Парение", "Падение", "Подъём", "Вихрь");
    public static final Setting.Num P_DENSITY = PART.num("density", "Плотность", 1, 20, 1, 3, "");
    public static final Setting.Num P_RADIUS = PART.num("radius", "Радиус", 4, 32, 2, 12, " м");
    public static final Setting.Num P_HEIGHT = PART.num("height", "Высота", 2, 20, 1, 8, " м");
    public static final Setting.Num P_SPEED = PART.num("speed", "Скорость", 0.2, 3, 0.1, 1, "x");
    public static final Setting.Bool P_NIGHT = PART.bool("night", "Только ночью", false);
    public static final Setting.Bool P_OUTDOOR = PART.bool("outdoor", "Только под открытым небом", false);


    public static final Feature TI = feat("tinfo", "Инфо о цели", "Визуалы", true, true);
    public static final Setting.Num TI_RANGE = TI.num("range", "Дальность", 5, 64, 1, 32, " м");
    public static final Setting.Mode TI_POS = TI.mode("pos", "Позиция", 0, "Справа от прицела", "Снизу", "Сверху", "Слева от прицела");
    public static final Setting.Mode TI_FILTER = TI.mode("filter", "Цели", 0, TARGET_FILTER);
    public static final Setting.Bool TI_DIST = TI.bool("dist", "Расстояние", true);
    public static final Setting.Bool TI_ARMORVAL = TI.bool("armorval", "Значение брони", true);
    public static final Setting.Bool TI_ARMOR = TI.bool("armor", "Надетая броня", true);
    public static final Setting.Bool TI_HANDS = TI.bool("hands", "Предметы в руках", true);
    public static final Setting.Bool TI_NAMES = TI.bool("names", "Названия предметов", true);
    public static final Setting.Bool TI_DUR = TI.bool("dur", "Прочность на иконках", true);
    public static final Setting.Bool TI_THROUGH = TI.bool("through", "Сквозь блоки", false);
    public static final Setting.Num TI_HOLD = TI.num("hold", "Задержка", 0, 3, 0.5, 1, " с");
    public static final Setting.Num TI_RADIUS = TI.num("radius", "Скругление", 0, 10, 1, 6, "");
    public static final Setting.Num TI_OPACITY = TI.num("opacity", "Прозрачность", 10, 100, 5, 75, "%");

    // ===================== ФИГУРА (косметика) =====================
    public static final Feature COS = feat("cos", "Общие", "Фигура", false, false);
    public static final Setting.Mode COS_WHO = COS.mode("who", "Кому показывать", 0, "Только мне", "Всем игрокам");
    public static final Setting.Info COS_HINT = COS.info(() -> "Видно от третьего лица (F5)");
    public static final Setting.Action COS_OFF = COS.action("Выключить всю косметику", () -> {
        Features.WINGS.enabled.value = false;
        Features.HAT.enabled.value = false;
        Features.HALO.enabled.value = false;
        Features.HORNS.enabled.value = false;
        Features.EARS.enabled.value = false;
        Features.TAIL.enabled.value = false;
        Config.save();
    });

    public static final Feature WINGS = feat("wings", "Крылья", "Фигура", true, false);
    public static final Setting.Mode CW_STYLE = WINGS.mode("style", "Стиль", 0, "Ангельские", "Демон", "Бабочка", "Лучи");
    public static final Setting.Color CW_C1 = WINGS.color("c1", "Основной цвет", 8);
    public static final Setting.Color CW_C2 = WINGS.color("c2", "Второй цвет", 2);
    public static final Setting.Bool CW_RAINBOW = WINGS.bool("rainbow", "Радуга", false);
    public static final Setting.Num CW_SIZE = WINGS.num("size", "Размер", 0.5, 2, 0.1, 1, "x");
    public static final Setting.Num CW_OPEN = WINGS.num("open", "Раскрытие", 0, 80, 5, 35, "°");
    public static final Setting.Num CW_FLAP = WINGS.num("flap", "Взмах", 0, 30, 1, 12, "°");
    public static final Setting.Num CW_SPEED = WINGS.num("speed", "Скорость взмаха", 0, 6, 0.5, 2, "x");
    public static final Setting.Num CW_OPACITY = WINGS.num("opacity", "Непрозрачность", 20, 100, 5, 90, "%");

    public static final Feature HAT = feat("hat", "Шляпа", "Фигура", true, false);
    public static final Setting.Mode CH_STYLE = HAT.mode("style", "Стиль", 0, "Китайская", "Ведьмина", "Корона", "Цилиндр", "Колпак");
    public static final Setting.Color CH_C1 = HAT.color("c1", "Основной цвет", 4);
    public static final Setting.Color CH_C2 = HAT.color("c2", "Второй цвет", 5);
    public static final Setting.Bool CH_RAINBOW = HAT.bool("rainbow", "Радуга", false);
    public static final Setting.Num CH_SIZE = HAT.num("size", "Размер", 0.5, 2, 0.1, 1, "x");
    public static final Setting.Num CH_HEIGHT = HAT.num("height", "Высота", 0.5, 2, 0.1, 1, "x");
    public static final Setting.Num CH_LIFT = HAT.num("lift", "Подъём над головой", -0.1, 0.3, 0.01, 0.03, "");
    public static final Setting.Num CH_OPACITY = HAT.num("opacity", "Непрозрачность", 20, 100, 5, 100, "%");

    public static final Feature HALO = feat("halo", "Нимб", "Фигура", true, false);
    public static final Setting.Color HL_C1 = HALO.color("c1", "Цвет", 4);
    public static final Setting.Bool HL_RAINBOW = HALO.bool("rainbow", "Радуга", false);
    public static final Setting.Num HL_SIZE = HALO.num("size", "Размер", 0.5, 2, 0.1, 1, "x");
    public static final Setting.Num HL_LIFT = HALO.num("lift", "Высота", 0.05, 0.6, 0.05, 0.25, "");
    public static final Setting.Num HL_BOB = HALO.num("bob", "Покачивание", 0, 0.1, 0.01, 0.03, "");
    public static final Setting.Num HL_OPACITY = HALO.num("opacity", "Непрозрачность", 20, 100, 5, 90, "%");

    public static final Feature HORNS = feat("horns", "Рожки", "Фигура", true, false);
    public static final Setting.Mode HR_STYLE = HORNS.mode("style", "Форма", 0, "Прямые", "Изогнутые");
    public static final Setting.Color HR_C1 = HORNS.color("c1", "Основной цвет", 6);
    public static final Setting.Color HR_C2 = HORNS.color("c2", "Цвет кончиков", 8);
    public static final Setting.Num HR_SIZE = HORNS.num("size", "Размер", 0.5, 2, 0.1, 1, "x");
    public static final Setting.Num HR_OPACITY = HORNS.num("opacity", "Непрозрачность", 20, 100, 5, 100, "%");

    public static final Feature EARS = feat("ears", "Кошачьи ушки", "Фигура", true, false);
    public static final Setting.Color EA_C1 = EARS.color("c1", "Цвет ушек", 7);
    public static final Setting.Color EA_C2 = EARS.color("c2", "Цвет внутри", 8);
    public static final Setting.Num EA_SIZE = EARS.num("size", "Размер", 0.5, 2, 0.1, 1, "x");
    public static final Setting.Bool EA_TWITCH = EARS.bool("twitch", "Подёргивание", true);
    public static final Setting.Num EA_OPACITY = EARS.num("opacity", "Непрозрачность", 20, 100, 5, 100, "%");

    public static final Feature TAIL = feat("tail", "Хвост", "Фигура", true, false);
    public static final Setting.Mode TL_STYLE = TAIL.mode("style", "Вид", 0, "Кошачий", "Лисий");
    public static final Setting.Color TL_C1 = TAIL.color("c1", "Основной цвет", 7);
    public static final Setting.Color TL_C2 = TAIL.color("c2", "Цвет кончика", 8);
    public static final Setting.Bool TL_RAINBOW = TAIL.bool("rainbow", "Радуга", false);
    public static final Setting.Num TL_LENGTH = TAIL.num("length", "Длина", 0.5, 2, 0.1, 1, "x");
    public static final Setting.Num TL_SWING = TAIL.num("swing", "Размах", 0, 1, 0.1, 0.6, "");
    public static final Setting.Num TL_OPACITY = TAIL.num("opacity", "Непрозрачность", 20, 100, 5, 100, "%");


    // ===================== ИГРА =====================
    public static final Feature AR = feat("aspect", "Аспект", "Игра", true, false);
    public static final Setting.Mode AR_PRESET = AR.mode("preset", "Соотношение сторон", 0,
            "4:3", "5:4", "16:10", "16:9", "21:9", "3:2", "1:1", "32:9", "Свой");
    public static final Setting.Num AR_CUSTOM = AR.num("custom", "Своё значение (ширина/высота)", 0.5, 3.5, 0.01, 1.33, "");
    public static final Setting.Bool AR_SMOOTH = AR.bool("smooth", "Плавный переход", true);

    public static final Feature MB = feat("mblur", "Motion Blur", "Игра", true, false);
    public static final Setting.Num MB_STR = MB.num("str", "Сила размытия", 10, 90, 5, 55, "%");
    public static final Setting.Bool MB_REACT = MB.bool("react", "Усиливать при движении камеры", true);
    public static final Setting.Num MB_MIN = MB.num("min", "Размытие в покое", 0, 60, 5, 10, "%");
    public static final Setting.Bool MB_ADAPT = MB.bool("adapt", "Подстраиваться под FPS", true);

    public static final Feature OPT = feat("opt", "Оптимизация", "Игра", true, false);
    public static final Setting.Bool OPT_VIEW_ON = OPT.bool("viewon", "Ограничить дальность прорисовки", true);
    public static final Setting.Num OPT_VIEW = OPT.num("view", "Дальность прорисовки", 2, 32, 1, 10, " чанк.");
    public static final Setting.Bool OPT_SIM_ON = OPT.bool("simon", "Ограничить дистанцию симуляции", true);
    public static final Setting.Num OPT_SIM = OPT.num("sim", "Дистанция симуляции", 5, 32, 1, 8, " чанк.");
    public static final Setting.Bool OPT_ENT_ON = OPT.bool("enton", "Ограничить дистанцию сущностей", false);
    public static final Setting.Num OPT_ENT = OPT.num("ent", "Дистанция сущностей", 50, 100, 5, 70, "%");
    public static final Setting.Bool OPT_CLOUDS = OPT.bool("clouds", "Отключить облака", true);
    public static final Setting.Mode OPT_PART = OPT.mode("part", "Частицы игры", 1, "Не менять", "Уменьшенные", "Минимальные");
    public static final Setting.Bool OPT_SHADOW = OPT.bool("shadow", "Отключить тени сущностей", true);
    public static final Setting.Bool OPT_BLEND = OPT.bool("blend", "Минимальное смешивание биомов", true);
    public static final Setting.Bool OPT_FAST = OPT.bool("fast", "Быстрая графика", false);
    public static final Setting.Bool OPT_VSYNC = OPT.bool("vsync", "Выключить VSync", false);
    public static final Setting.Bool OPT_FPS_ON = OPT.bool("fpson", "Лимит FPS", false);
    public static final Setting.Num OPT_FPS = OPT.num("fps", "Максимум FPS (260 = без лимита)", 30, 260, 10, 144, "");
    public static final Setting.Bool OPT_ADAPT = OPT.bool("adapt", "Адаптивные частицы мода", true);
    public static final Setting.Num OPT_ADAPT_FPS = OPT.num("adaptfps", "Целевой FPS", 20, 120, 5, 60, "");

    public static final Feature HAND = feat("hand", "Thing Animation", "Игра", true, false);
    public static final Setting.Mode HA_APPLY = HAND.mode("apply", "Применять к", 0, "Руке и предмету", "Только предметам", "Только руке");
    public static final Setting.Num HA_X = HAND.num("x", "Влево / вправо", -1, 1, 0.02, 0, "");
    public static final Setting.Num HA_Y = HAND.num("y", "Вниз / вверх", -1, 1, 0.02, 0, "");
    public static final Setting.Num HA_Z = HAND.num("z", "Ближе / дальше", -1, 1, 0.02, 0, "");
    public static final Setting.Num HA_SCALE = HAND.num("scale", "Размер", 0.2, 2.5, 0.05, 1, "x");
    public static final Setting.Num HA_RX = HAND.num("rx", "Наклон вперёд / назад", -90, 90, 5, 0, "°");
    public static final Setting.Num HA_RY = HAND.num("ry", "Поворот в стороны", -90, 90, 5, 0, "°");
    public static final Setting.Num HA_RZ = HAND.num("rz", "Крен", -90, 90, 5, 0, "°");
    public static final Setting.Bool HA_MIRROR = HAND.bool("mirror", "Зеркалить для левой руки", true);
    public static final Setting.Bool HA_OFFHAND = HAND.bool("offhand", "Применять к левой руке", true);
    public static final Setting.Bool HA_EQUIP = HAND.bool("equip", "Плавная смена предмета", false);
    public static final Setting.Bool HA_ANIM = HAND.bool("anim", "Своя анимация удара", false);
    public static final Setting.Mode HA_TYPE = HAND.mode("type", "Тип анимации", 0,
            "Ванильный", "Mode 1 (снизу вверх)", "Mode 2 (горизонтальный)", "HMI (вперёд)", "Сверху вниз",
            "Блок-хит 1.8", "Маятник", "Круговой", "Плавный взмах");
    public static final Setting.Mode HA_EASE = HAND.mode("ease", "Плавность", 0,
            "Ease out", "Ease in-out", "Ease in", "Линейно", "Пружина");
    public static final Setting.Num HA_TIME = HAND.num("time", "Длительность", 100, 1200, 25, 350, " мс");
    public static final Setting.Num HA_POWER = HAND.num("power", "Сила", 0, 2, 0.1, 1, "x");
    public static final Setting.Bool HA_SUPPRESS = HAND.bool("suppress", "Отключить ванильный взмах", true);
    public static final Setting.Action HA_P1 = HAND.action("Пресет: классика 1.8", () -> preset(0.10, -0.12, 0.05, 0.9, 0, -10, 5));
    public static final Setting.Action HA_P2 = HAND.action("Пресет: низко справа", () -> preset(0.20, -0.25, 0, 1.0, 0, 0, 0));
    public static final Setting.Action HA_P3 = HAND.action("Пресет: по центру", () -> preset(-0.55, 0.0, 0.05, 1.0, 0, 0, 0));
    public static final Setting.Action HA_P4 = HAND.action("Пресет: маленькая", () -> preset(0.05, -0.05, 0, 0.6, 0, 0, 0));
    public static final Setting.Action HA_P5 = HAND.action("Пресет: большая", () -> preset(0.10, 0.0, 0, 1.5, 0, 0, 0));
    public static final Setting.Action HA_P6 = HAND.action("Сбросить позицию", () -> preset(0, 0, 0, 1.0, 0, 0, 0));

    private static void preset(double x, double y, double z, double sc, double rx, double ry, double rz) {
        Features.HA_X.set(x);
        Features.HA_Y.set(y);
        Features.HA_Z.set(z);
        Features.HA_SCALE.set(sc);
        Features.HA_RX.set(rx);
        Features.HA_RY.set(ry);
        Features.HA_RZ.set(rz);
        Config.save();
    }

    // ===================== ИНТЕРФЕЙС =====================
    public static final Feature WM = feat("watermark", "Вотермарка", "Интерфейс", true, true);
    public static final Setting.Mode W_STYLE = WM.mode("style", "Стиль", 0, BG_STYLES);
    public static final Setting.Mode W_POS = WM.mode("pos", "Позиция", 0, "Верх центр", "Верх слева", "Верх справа");
    public static final Setting.Bool W_NAME = WM.bool("name", "Название", true);
    public static final Setting.Bool W_USER = WM.bool("user", "Ник", true);
    public static final Setting.Bool W_FPS = WM.bool("fps", "FPS", true);
    public static final Setting.Bool W_PING = WM.bool("ping", "Пинг", true);
    public static final Setting.Bool W_TIME = WM.bool("time", "Время", true);
    public static final Setting.Bool W_LINE = WM.bool("line", "Акцентная линия", true);
    public static final Setting.Num W_RADIUS = WM.num("radius", "Скругление", 0, 10, 1, 6, "");
    public static final Setting.Num W_OPACITY = WM.num("opacity", "Прозрачность", 10, 100, 5, 70, "%");

    public static final Feature COORDS = feat("coords", "Координаты", "Интерфейс", true, true);
    public static final Setting.Mode C_POS = COORDS.mode("pos", "Позиция", 0, "Лево низ", "Лево верх", "Право низ");
    public static final Setting.Mode C_STYLE = COORDS.mode("style", "Стиль", 0, BG_STYLES);
    public static final Setting.Bool C_DEC = COORDS.bool("dec", "Дробные координаты", false);
    public static final Setting.Bool C_DIR = COORDS.bool("dir", "Направление", true);
    public static final Setting.Bool C_BIOME = COORDS.bool("biome", "Биом", false);
    public static final Setting.Bool C_NETHER = COORDS.bool("nether", "Координаты другого мира", false);
    public static final Setting.Bool C_SPEED = COORDS.bool("speed", "Скорость", false);
    public static final Setting.Num C_RADIUS = COORDS.num("radius", "Скругление", 0, 10, 1, 6, "");
    public static final Setting.Num C_OPACITY = COORDS.num("opacity", "Прозрачность", 10, 100, 5, 60, "%");

    public static final Feature EFFECTS = feat("effects", "Эффекты", "Интерфейс", true, true);
    public static final Setting.Mode E_POS = EFFECTS.mode("pos", "Позиция", 0, "Право верх", "Лево верх");
    public static final Setting.Mode E_STYLE = EFFECTS.mode("style", "Стиль", 0, BG_STYLES);
    public static final Setting.Bool E_TIMER = EFFECTS.bool("timer", "Таймер", true);
    public static final Setting.Bool E_LEVEL = EFFECTS.bool("level", "Уровень", true);
    public static final Setting.Bool E_DOT = EFFECTS.bool("dot", "Цветная точка", true);
    public static final Setting.Bool E_BLINK = EFFECTS.bool("blink", "Мигать в конце", true);
    public static final Setting.Num E_RADIUS = EFFECTS.num("radius", "Скругление", 0, 10, 1, 6, "");
    public static final Setting.Num E_OPACITY = EFFECTS.num("opacity", "Прозрачность", 10, 100, 5, 60, "%");

    public static final Feature ARMOR = feat("armor", "Броня", "Интерфейс", true, false);
    public static final Setting.Mode A_POS = ARMOR.mode("pos", "Позиция", 0, "Лево центр", "Право центр");
    public static final Setting.Mode A_DUR = ARMOR.mode("dur", "Прочность", 1, "Полоска", "Процент", "Число", "Нет");
    public static final Setting.Bool A_HAND = ARMOR.bool("hand", "Предмет в руке", true);
    public static final Setting.Bool A_BG = ARMOR.bool("bg", "Фон", true);
    public static final Setting.Num A_OPACITY = ARMOR.num("opacity", "Прозрачность", 10, 100, 5, 60, "%");


    public static final Feature CROSS = feat("cross", "Прицел", "Интерфейс", true, true);
    public static final Setting.Mode X_STYLE = CROSS.mode("style", "Стиль", 0, "Крест", "Точка", "Круг", "Крест и точка",
            "Квадрат", "Ромб", "Т-образный", "Уголки", "Звезда", "Круг с крестом");
    public static final Setting.Color X_COLOR = CROSS.color("color", "Цвет", 0);
    public static final Setting.Bool X_RAINBOW = CROSS.bool("rainbow", "Радуга", false);
    public static final Setting.Num X_SIZE = CROSS.num("size", "Размер", 1, 20, 1, 5, "");
    public static final Setting.Num X_GAP = CROSS.num("gap", "Зазор в центре", 0, 12, 1, 3, "");
    public static final Setting.Num X_THICK = CROSS.num("thick", "Толщина", 1, 4, 1, 1, "");
    public static final Setting.Bool X_OUTLINE = CROSS.bool("outline", "Чёрная обводка", true);
    public static final Setting.Bool X_DOT = CROSS.bool("dot", "Точка в центре", false);
    public static final Setting.Num X_OPACITY = CROSS.num("opacity", "Непрозрачность", 20, 100, 5, 100, "%");
    public static final Setting.Bool X_DYNAMIC = CROSS.bool("dynamic", "Расходится при движении", true);
    public static final Setting.Num X_DYN_AMT = CROSS.num("dynamt", "Сила расхождения", 0, 10, 1, 4, "");
    public static final Setting.Bool X_TARGETCOL = CROSS.bool("targetcol", "Красный на цели", true);
    public static final Setting.Bool X_HITMARK = CROSS.bool("hitmark", "Хитмаркер при ударе", true);
    public static final Setting.Color X_HITCOL = CROSS.color("hitcol", "Цвет хитмаркера", 8);
    public static final Setting.Num X_SPIN = CROSS.num("spin", "Вращение", 0, 8, 1, 0, "");
    public static final Setting.Bool X_HIDE = CROSS.bool("hide", "Скрыть ванильный прицел", true);

    public static final Feature CPS = feat("cps", "Счётчик CPS", "Интерфейс", true, false);
    public static final Setting.Mode CPS_POS = CPS.mode("pos", "Позиция", 0, "Право низ", "Лево верх", "Под прицелом");
    public static final Setting.Bool CPS_RMB = CPS.bool("rmb", "Показывать правую кнопку", true);
    public static final Setting.Num CPS_RADIUS = CPS.num("radius", "Скругление", 0, 10, 1, 6, "");
    public static final Setting.Num CPS_OPACITY = CPS.num("opacity", "Прозрачность", 10, 100, 5, 60, "%");

    // ===================== ТЕМА =====================
    public static final Feature TH_COLORS = feat("th_colors", "Цвета", "Тема", false, false);
    public static final Setting.Mode T_THEME = TH_COLORS.mode("theme", "Тема", 0, Theme.NAMES);
    public static final Setting.Color T_ACCENT = TH_COLORS.color("accent", "Акцент", 0);

    public static final Feature TH_SHAPE = feat("th_shape", "Форма", "Тема", false, false);
    public static final Setting.Num T_RADIUS = TH_SHAPE.num("radius", "Скругление", 0, 14, 1, 10, " px");
    public static final Setting.Mode T_SIZE = TH_SHAPE.mode("size", "Размер окна", 1, "Малый", "Обычный", "Большой");
    public static final Setting.Mode T_DENSITY = TH_SHAPE.mode("density", "Плотность строк", 1, "Компактно", "Обычно", "Просторно");

    public static final Feature TH_GLASS = feat("th_glass", "Жидкое стекло", "Тема", false, false);
    public static final Setting.Bool T_BLUR = TH_GLASS.bool("blur", "Размытие фона", true);
    public static final Setting.Num T_OPACITY = TH_GLASS.num("opacity", "Прозрачность окна", 20, 95, 1, 62, "%");
    public static final Setting.Bool T_GLOW = TH_GLASS.bool("glow", "Свет сверху", true);
    public static final Setting.Bool T_SHEEN = TH_GLASS.bool("sheen", "Бегущий отсвет", true);
    public static final Setting.Num T_SHEEN_STR = TH_GLASS.num("sheenstr", "Сила отсвета", 0, 100, 5, 45, "%");
    public static final Setting.Bool T_BORDER = TH_GLASS.bool("border", "Светлая обводка", true);
    public static final Setting.Num T_BORDER_STR = TH_GLASS.num("borderstr", "Яркость обводки", 10, 100, 5, 60, "%");

    public static final Feature TH_ANIM = feat("th_anim", "Анимации", "Тема", false, false);
    public static final Setting.Bool T_ANIM = TH_ANIM.bool("on", "Плавные анимации", true);
    public static final Setting.Num T_SPEED = TH_ANIM.num("speed", "Скорость", 0.5, 3, 0.1, 1, "x");
    public static final Setting.Mode T_APPEAR = TH_ANIM.mode("appear", "Появление", 0, "Масштаб", "Снизу", "Затухание");

    static {
        T_SHEEN_STR.showIf(() -> T_SHEEN.value);
        T_BORDER_STR.showIf(() -> T_BORDER.value);
        T_SPEED.showIf(() -> T_ANIM.value);
        T_APPEAR.showIf(() -> T_ANIM.value);
        H_DEATH_MULT.showIf(() -> H_DEATH.value);
        AR_CUSTOM.showIf(() -> AR_PRESET.index == 8);
        OPT_VIEW.showIf(() -> OPT_VIEW_ON.value);
        OPT_SIM.showIf(() -> OPT_SIM_ON.value);
        OPT_ENT.showIf(() -> OPT_ENT_ON.value);
        OPT_FPS.showIf(() -> OPT_FPS_ON.value);
        OPT_ADAPT_FPS.showIf(() -> OPT_ADAPT.value);
        MB_MIN.showIf(() -> MB_REACT.value);
        HA_TYPE.showIf(() -> HA_ANIM.value);
        HA_EASE.showIf(() -> HA_ANIM.value);
        HA_TIME.showIf(() -> HA_ANIM.value);
        HA_POWER.showIf(() -> HA_ANIM.value);
        HA_SUPPRESS.showIf(() -> HA_ANIM.value);
    }

    public static void resetAll() {
        for (Feature f : ALL) {
            if (f.enabled != null) f.enabled.reset();
            for (Setting s : f.settings) s.reset();
        }
    }
}
