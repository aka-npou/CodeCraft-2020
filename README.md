# CodeCraft-2020
Возможно когда-то тут появится исходный код.

https://russianaicup.ru/profile/ThermIt

Бот занял прошёл в финал RAIC-2020, занял там 18 место и после оптимизации первоначальных приказов на постройку зданий занял 11(6 без победителей финала) призовое место в песочнице.

## Алгоритм бота:
### Инициализация данных:
1. Entity фильтруются в отдельные коллекции по типам + свои-чужие для легкого получения в виде списка.
2. Строится карта всех видимых единиц Entity[MAP_SIZE][MAP_SIZE].
    1. При действующем тумане войны к карте прилагаются юниты врагов, которые пропали из видимости, но не были убиты и 1 или 3 существующие базы противников.
3. Строятся "выталкивающие" ПП (Потенциальные поля) вокруг врагов на дистанцию их атаки (+)возможность их передвижения в следующий тик (-)их занятость атакой на  что-то (-)если у наших рядом больше 60hp.
    1. Поля устроены таким образом, что поля одного юнита не достаточно для выталкивания. Для выталкивания суммарные потенциальные повреждения должны превысить здоровье выталкиваемого юнита. То есть ни один юнит сам по себе не должен идти в точку, где его убьют в 1 тик если это не является более массовой атакой.
4. Строятся карты расстояний до ближайшего врага из каждой точки карты (туррели игнорируются пока есть другие юниты).
5. Строятся карты расстояний до ближайшего вражеского рабочего/здания из каждой точки карты (туррели игнорируются пока есть другие юниты).
6. Строится карта ресурсов и видимости, ресурсы отзеркаливаются и расставляются фальшивые ресурсы в точках, которые я никогда не видел.
7. Строятся карты расстояний до ближайшего "безопасного" не занятого ресурса из каждой точки карты в нескольких вариациях "занятости".
8. Строится карта свободных пространств (или занятых "движимым имуществом") в каждой точке карты (площадь пустого квадрата в направлении возрастания координат).
9. Обновляется очередь постройки на основе близости до рабочих. Старые приказы на постройку выкидываются если стали не актуальны. Если видно, что текущих ресурсов хватает, чтоб выйти за лимит, то добавляются новые приказы на постройку домов (для этого перебираются все подходящие пустые пространства не далее 15 тиков от рабочих). Строятся карты расстояний от активных приказов на постройку до ближайших рабочих.
10. Строится карта расстояний до наших юнитов атакующих юнитов врага для их починки.

### Юниты делятся на несколько "классов":
* melee: сюда относятся все melee юниты и рабочие, у которых кончились задания. Эти юниты просто идут к ближайшему врагу.
* healer: пара рабочих, которые бегают и превентивно лечат своих юнитов во время атаки и долечивают после боя.
* attackers: 1/3 ranged бойцов, идущих по центральной диагонали на ближайшего врага.
* harassers-1: 1/3 ranged бойцов, идущих по верху карты на ближайшего вражеского рабочего/здания (сначала вверх потом направо).
  * В конце был вкручен костыль для harassers бежать к юнитам, которые атакуют наших рабочих, если такие есть.
* harassers-2: 1/3 ranged бойцов, идущих по низу карты на ближайшего вражеского рабочего/здания (сначала направо потом вверх).
* harvesters: основной пул рабочих, занятых добычей.
* builders: рабочие, ближайшие к активным приказам на постройку из очереди.

### Действия:
1. Если кто-то может атаковать/чинить - отдаётся приказ на атаку/добычу/починку без превышения максимально возможного урона
2. Остальным отдаётся приказ на передвижение в направлении в соответствии с его текущей позицией и текущим назначенным "классом" юнита. Каждому "классу" юнита соответствует своя карта расстояний, которая преобразуется однозначно в векторную карту в направлении уменьшения дистанции.
3. Если после выполнения приказа юнит попадёт в действие ПП, то отдаётся приоритет ПП и юнит "выталкивается" обратно.
4. Если юниту кто-то мешает, то управления передаётся сначала тому, кто мешает, при попытке передать управление назад считается что место занято и ищется другой возможный ход для разрыва круга.
5. Если рабочему мешает двигаться другой рабочий, то текущий рабочий перетирает приказ того рабочего на движение в соответствии со своим заданием.

### Строительство юнитов:
1. Во всех зданиях строим всех юнитов, но не более 5 melee, не более 80 рабочих, если рабочих более 40 и на нашей половине карты противник - не строим рабочих.

### Повторить 1000 раз

## Что больше всего повлияло на занятие более-менее нормального места в песочнице и проход в финал:
1. Оптимизация стартовых построек.
2. Постройка рабочих/солдат в позиции с учётом их ожидаемой задачи.
3. Выталкивающие ПП атакующих юнитов врагов.
4. Толкание рабочими друг-друга.
5. Повышенный приоритет уничтожения рабочих противника.
6. Приоритет постройки в местах, где много рабочих могут строить.
7. Куча мелких if-ов, каждый из которых ничего не значит в отдельности.
8. Применение 5 melee юнитов в играх первого раунда, которых многие игнорировали.

## Что хотелось реализовать, но не хватило времени
1. Пул задач и более детальное распределение задач по одной с просчётом в будущее. Создание юнитов под определённые задачи в определённый тик для экономии ресурсов.
2. Переписать все поиски пути с общих векторных полей на отдельные BFS каждому юниту в соответствии с его задачей с учётом занимаемого на каждом тике пространства. Данное изменение потребовало бы полного переписывания бота. Но небольшие тесты показывали существенное преимущество по сравнению с реализованным подходом. К сожалению, без использования GraalVM изначально все попытки сделать на Java подобный подход уходили в жёсткий тайм лимит.
3. Создание приказов на постройку зданий в определённый тик с блокировкой рассчитанных ресурсов и пересчётом движения всех юнитов, которым это здание помешает в будущем.
4. Продвижение атакующих юнитов вперёд если размен обещает быть выгодным.
5. Более детальное распределение задач на атаку-защиту. Разделение боевых единиц на 3 атакующие группы явно не достаточно.
6. Максимизация/минимизация повреждений при атаках.
7. Симуляция и оптимизация всех действий до встречи с противником для исключения бесполезных действий.
