-- Insert test data for Team_Dim table
-- 5-level organizational hierarchy with themed team names
-- All teams have explicit primary keys for easier fact table creation

-- Clear existing data
DELETE FROM "prism"."Team_Dim";

-- Insert teams with 5-level hierarchy and explicit primary keys
-- Level 5: CEO, Level 4: SVPs, Level 3: Directors, Level 2: Senior Managers, Level 1: Managers

INSERT INTO "prism"."Team_Dim" 
(team_pk, team_name, level_1_name, level_2_name, level_3_name, level_4_name, level_5_name)
VALUES
-- SVP 1: David Thompson
-- Director 1: Jennifer Kim
-- Senior Manager 1: Michael Rodriguez
-- Manager 1: Sarah Chen (Space Shuttle theme)
(1, 'Discovery', 'Sarah Chen', 'Michael Rodriguez', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),
(2, 'Endeavour', 'Sarah Chen', 'Michael Rodriguez', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),
(3, 'Atlantis', 'Sarah Chen', 'Michael Rodriguez', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),

-- Manager 2: James Wilson (Mountain theme)
(4, 'Everest', 'James Wilson', 'Michael Rodriguez', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),
(5, 'Kilimanjaro', 'James Wilson', 'Michael Rodriguez', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),

-- Manager 3: Lisa Garcia (Helicopter theme)
(6, 'Apache', 'Lisa Garcia', 'Michael Rodriguez', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),
(7, 'Cobra', 'Lisa Garcia', 'Michael Rodriguez', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),

-- Senior Manager 2: Amanda White
-- Manager 4: Kevin Lee (War General theme)
(8, 'Grant', 'Kevin Lee', 'Amanda White', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),
(9, 'Patton', 'Kevin Lee', 'Amanda White', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),

-- Manager 5: Maria Santos (Cartoon Character theme)
(10, 'Flintstone', 'Maria Santos', 'Amanda White', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),
(11, 'Jetson', 'Maria Santos', 'Amanda White', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),

-- Manager 6: Alex Johnson (National Park theme)
(12, 'Yellowstone', 'Alex Johnson', 'Amanda White', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),
(13, 'Yosemite', 'Alex Johnson', 'Amanda White', 'Jennifer Kim', 'David Thompson', 'Robert Anderson'),

-- Director 2: Christopher Lee
-- Senior Manager 3: Rachel Brown
-- Manager 7: Daniel Kim (Star Trek theme)
(14, 'Enterprise', 'Daniel Kim', 'Rachel Brown', 'Christopher Lee', 'David Thompson', 'Robert Anderson'),
(15, 'Reliant', 'Daniel Kim', 'Rachel Brown', 'Christopher Lee', 'David Thompson', 'Robert Anderson'),

-- Manager 8: Nicole Davis (US State theme)
(16, 'Virginia', 'Nicole Davis', 'Rachel Brown', 'Christopher Lee', 'David Thompson', 'Robert Anderson'),
(17, 'Montana', 'Nicole Davis', 'Rachel Brown', 'Christopher Lee', 'David Thompson', 'Robert Anderson'),

-- Manager 9: Brian Martinez (WW2 Ship theme)
(18, 'Bismarck', 'Brian Martinez', 'Rachel Brown', 'Christopher Lee', 'David Thompson', 'Robert Anderson'),
(19, 'Yamato', 'Brian Martinez', 'Rachel Brown', 'Christopher Lee', 'David Thompson', 'Robert Anderson'),

-- Senior Manager 4: Jessica Thompson
-- Manager 10: Ryan Clark (Modern Battleship theme)
(20, 'Nimitz', 'Ryan Clark', 'Jessica Thompson', 'Christopher Lee', 'David Thompson', 'Robert Anderson'),
(21, 'Roosevelt', 'Ryan Clark', 'Jessica Thompson', 'Christopher Lee', 'David Thompson', 'Robert Anderson'),

-- Manager 11: Stephanie Adams (Cartoon Character theme)
(22, 'Griffin', 'Stephanie Adams', 'Jessica Thompson', 'Christopher Lee', 'David Thompson', 'Robert Anderson'),
(23, 'Belcher', 'Stephanie Adams', 'Jessica Thompson', 'Christopher Lee', 'David Thompson', 'Robert Anderson'),

-- Manager 12: Mark Wilson (National Park theme)
(24, 'Grand Canyon', 'Mark Wilson', 'Jessica Thompson', 'Christopher Lee', 'David Thompson', 'Robert Anderson'),
(25, 'Zion', 'Mark Wilson', 'Jessica Thompson', 'Christopher Lee', 'David Thompson', 'Robert Anderson'),

-- SVP 2: Elizabeth Johnson
-- Director 3: Michael Chen
-- Senior Manager 5: David Park
-- Manager 13: Lauren Miller (Helicopter theme)
(26, 'Blackhawk', 'Lauren Miller', 'David Park', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),
(27, 'Chinook', 'Lauren Miller', 'David Park', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),
(28, 'Kiowa', 'Lauren Miller', 'David Park', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),

-- Manager 14: Thomas Anderson (War General theme)
(29, 'Eisenhower', 'Thomas Anderson', 'David Park', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),
(30, 'MacArthur', 'Thomas Anderson', 'David Park', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),

-- Manager 15: Jennifer Lee (US State theme)
(31, 'Alaska', 'Jennifer Lee', 'David Park', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),
(32, 'Hawaii', 'Jennifer Lee', 'David Park', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),
(33, 'Texas', 'Jennifer Lee', 'David Park', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),

-- Senior Manager 6: Sarah Williams
-- Manager 16: Ryan Taylor (Star Trek theme)
(34, 'Defiant', 'Ryan Taylor', 'Sarah Williams', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),
(35, 'Voyager', 'Ryan Taylor', 'Sarah Williams', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),

-- Manager 17: Amanda Garcia (WW2 Ship theme)
(36, 'Hood', 'Amanda Garcia', 'Sarah Williams', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),
(37, 'Tirpitz', 'Amanda Garcia', 'Sarah Williams', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),

-- Manager 18: Kevin Brown (Modern Battleship theme)
(38, 'Lincoln', 'Kevin Brown', 'Sarah Williams', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),
(39, 'Truman', 'Kevin Brown', 'Sarah Williams', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),
(40, 'Bush', 'Kevin Brown', 'Sarah Williams', 'Michael Chen', 'Elizabeth Johnson', 'Robert Anderson'),

-- Director 4: Lisa Martinez
-- Senior Manager 7: Chris Davis
-- Manager 19: Nicole Wilson (Space Shuttle theme)
(41, 'Challenger', 'Nicole Wilson', 'Chris Davis', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),
(42, 'Columbia', 'Nicole Wilson', 'Chris Davis', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),

-- Manager 20: Brian Kim (Mountain theme)
(43, 'Denali', 'Brian Kim', 'Chris Davis', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),
(44, 'Aconcagua', 'Brian Kim', 'Chris Davis', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),

-- Manager 21: Jessica Adams (Cartoon Character theme)
(45, 'Simpson', 'Jessica Adams', 'Chris Davis', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),
(46, 'Griffin', 'Jessica Adams', 'Chris Davis', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),

-- Senior Manager 8: Mark Thompson
-- Manager 22: Stephanie Lee (National Park theme)
(47, 'Arches', 'Stephanie Lee', 'Mark Thompson', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),
(48, 'Sequoia', 'Stephanie Lee', 'Mark Thompson', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),

-- Manager 23: Daniel White (US State theme)
(49, 'California', 'Daniel White', 'Mark Thompson', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),
(50, 'Florida', 'Daniel White', 'Mark Thompson', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),

-- Manager 24: Maria Clark (Star Trek theme)
(51, 'Intrepid', 'Maria Clark', 'Mark Thompson', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),
(52, 'Prometheus', 'Maria Clark', 'Mark Thompson', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),
(53, 'Sovereign', 'Maria Clark', 'Mark Thompson', 'Lisa Martinez', 'Elizabeth Johnson', 'Robert Anderson'),

-- SVP 3: Robert Smith
-- Director 5: Jennifer Taylor
-- Senior Manager 9: Alex Rodriguez
-- Manager 25: Chris Johnson (Helicopter theme)
(54, 'Comanche', 'Chris Johnson', 'Alex Rodriguez', 'Jennifer Taylor', 'Robert Smith', 'Robert Anderson'),
(55, 'Lakota', 'Chris Johnson', 'Alex Rodriguez', 'Jennifer Taylor', 'Robert Smith', 'Robert Anderson'),

-- Manager 26: Rachel Martinez (War General theme)
(56, 'Sherman', 'Rachel Martinez', 'Alex Rodriguez', 'Jennifer Taylor', 'Robert Smith', 'Robert Anderson'),
(57, 'Bradley', 'Rachel Martinez', 'Alex Rodriguez', 'Jennifer Taylor', 'Robert Smith', 'Robert Anderson'),

-- Manager 27: Kevin Davis (US State theme)
(58, 'New York', 'Kevin Davis', 'Alex Rodriguez', 'Jennifer Taylor', 'Robert Smith', 'Robert Anderson'),
(59, 'Illinois', 'Kevin Davis', 'Alex Rodriguez', 'Jennifer Taylor', 'Robert Smith', 'Robert Anderson'),

-- Senior Manager 10: Lisa Brown
-- Manager 28: Amanda Wilson (Star Trek theme)
(60, 'Excelsior', 'Amanda Wilson', 'Lisa Brown', 'Jennifer Taylor', 'Robert Smith', 'Robert Anderson'),
(61, 'Constellation', 'Amanda Wilson', 'Lisa Brown', 'Jennifer Taylor', 'Robert Smith', 'Robert Anderson'),

-- Manager 29: Brian Garcia (WW2 Ship theme)
(62, 'Scharnhorst', 'Brian Garcia', 'Lisa Brown', 'Jennifer Taylor', 'Robert Smith', 'Robert Anderson'),
(63, 'Gneisenau', 'Brian Garcia', 'Lisa Brown', 'Jennifer Taylor', 'Robert Smith', 'Robert Anderson'),

-- Manager 30: Jessica Kim (Modern Battleship theme)
(64, 'Reagan', 'Jessica Kim', 'Lisa Brown', 'Jennifer Taylor', 'Robert Smith', 'Robert Anderson'),
(65, 'Ford', 'Jessica Kim', 'Lisa Brown', 'Jennifer Taylor', 'Robert Smith', 'Robert Anderson'),

-- Director 6: Michael White
-- Senior Manager 11: David Lee
-- Manager 31: Nicole Adams (Space Shuttle theme)
(66, 'Endeavour', 'Nicole Adams', 'David Lee', 'Michael White', 'Robert Smith', 'Robert Anderson'),
(67, 'Atlantis', 'Nicole Adams', 'David Lee', 'Michael White', 'Robert Smith', 'Robert Anderson'),

-- Manager 32: Thomas Martinez (Mountain theme)
(68, 'Vinson', 'Thomas Martinez', 'David Lee', 'Michael White', 'Robert Smith', 'Robert Anderson'),
(69, 'Elbrus', 'Thomas Martinez', 'David Lee', 'Michael White', 'Robert Smith', 'Robert Anderson'),

-- Manager 33: Jennifer Clark (Cartoon Character theme)
(70, 'Belcher', 'Jennifer Clark', 'David Lee', 'Michael White', 'Robert Smith', 'Robert Anderson'),
(71, 'Smith', 'Jennifer Clark', 'David Lee', 'Michael White', 'Robert Smith', 'Robert Anderson'),

-- Senior Manager 12: Mark Johnson
-- Manager 34: Stephanie Brown (National Park theme)
(72, 'Glacier', 'Stephanie Brown', 'Mark Johnson', 'Michael White', 'Robert Smith', 'Robert Anderson'),
(73, 'Olympic', 'Stephanie Brown', 'Mark Johnson', 'Michael White', 'Robert Smith', 'Robert Anderson'),

-- Manager 35: Daniel Garcia (US State theme)
(74, 'Pennsylvania', 'Daniel Garcia', 'Mark Johnson', 'Michael White', 'Robert Smith', 'Robert Anderson'),
(75, 'Ohio', 'Daniel Garcia', 'Mark Johnson', 'Michael White', 'Robert Smith', 'Robert Anderson'),
(76, 'Michigan', 'Daniel Garcia', 'Mark Johnson', 'Michael White', 'Robert Smith', 'Robert Anderson'),

-- Manager 36: Maria Kim (Star Trek theme)
(77, 'Akira', 'Maria Kim', 'Mark Johnson', 'Michael White', 'Robert Smith', 'Robert Anderson'),
(78, 'Nebula', 'Maria Kim', 'Mark Johnson', 'Michael White', 'Robert Smith', 'Robert Anderson');

-- Verify the data
SELECT 
    team_pk,
    team_name,
    level_1_name as manager,
    level_2_name as senior_manager,
    level_3_name as director,
    level_4_name as svp,
    level_5_name as ceo
FROM "prism"."Team_Dim"
ORDER BY team_pk;

SELECT 'Team dimension data with explicit primary keys inserted successfully!' as status;
